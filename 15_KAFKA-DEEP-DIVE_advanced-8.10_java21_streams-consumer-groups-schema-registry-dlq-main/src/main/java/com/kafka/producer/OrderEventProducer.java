package com.kafka.producer;

import com.kafka.model.OrderEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * KAFKA MESAJ ÜRETİCİSİ — Producer Desenleri ve Trade-off'lar
 * =============================================================
 *
 * Kafka nedir, HTTP yerine neden kullanılır?
 *   HTTP (senkron): A → B isteği gönderir. B düşükse A hata alır.
 *   Kafka (asenkron): A → Kafka'ya yazar. B hazır olunca Kafka'dan okur.
 *   Kafka'nın avantajları:
 *     - Dayanıklılık: Mesajlar diske yazılır, broker restart olsa kaybolmaz.
 *     - Ölçeklenebilirlik: Partition sayısı artırılarak yatay ölçeklenir.
 *     - Yeniden oynatma: Consumer'lar geçmiş mesajları tekrar okuyabilir.
 *     - Decoupling: Producer B'nin varlığından haberdar olmak zorunda değil.
 *
 * Gönderim yöntemleri karşılaştırması:
 *   Fire-and-forget: Hızlı, düşük CPU — ama kayıp riski (kritik işlemlerde kullanma).
 *   Async + callback: Hızlı + hata durumu yönetilir — önerilen yöntem.
 *   Sync: Güvenilir ama thread bloke olur — düşük throughput, dikkatli kullan.
 *
 * Partition kavramı:
 *   Topic N partition'a bölünür. Her partition sıralı, değiştirilemez log.
 *   Producer → hangi partition'a? → key hash (deterministic) veya round-robin.
 *   Aynı key → aynı partition → o key için sıra garantisi.
 *   Farklı key'ler farklı partition'larda olabilir — global sıra garantisi yok.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.topics.orders}")
    private String ordersTopic;

    @Value("${app.topics.payments}")
    private String paymentsTopic;

    @Value("${app.topics.dlq}")
    private String dlqTopic;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Fire-and-forget — En Hızlı, Kayıp Riski Var.
     *
     * Ne yapar?
     *   Mesajı Kafka'ya gönderir, callback yoktur — sonucu umursamaz.
     *   send() asenkron döner — thread bloke olmaz.
     *
     * Ne zaman kullanılır?
     *   Kayıp tolere edilebilen veriler: analitik event'leri, sayaçlar, loglar.
     *   "Bu sayfayı 1M kullanıcı ziyaret etti" — 1-2 kayıp önemsiz.
     *
     * Ne zaman KULLANILMAZ?
     *   Sipariş, ödeme, stok değişimi: Her event kritik → kayıp kabul edilemez.
     *   Bu durumlar için: async + callback veya sync kullan.
     *
     * key = event.getCustomerId(): Aynı müşterinin event'leri aynı partition'a gider.
     *   Neden? Müşteri A'nın event'leri sıralı olmalı (ORDER_PLACED → ORDER_CONFIRMED).
     *   Farklı partition'da olsaydı: Consumer'lar farklı hızda işler → sıra bozulur.
     */
    public void sendFireAndForget(OrderEvent event) {
        kafkaTemplate.send(ordersTopic, event.getCustomerId(), event);
        log.info("Fire-and-forget: {}", event.getOrderId());
    }

    /**
     * Async + Callback — Önerilen Yöntem.
     *
     * Neden async + callback?
     *   Fire-and-forget: Hata olursa bilmeyiz.
     *   Sync: Thread bloke olur — yavaş.
     *   Async + callback: Hızlı gönderim + hata durumunu yakala → DLQ'ya gönder.
     *
     * CompletableFuture:
     *   kafkaTemplate.send() → hemen döner (non-blocking).
     *   whenComplete: Broker yanıt verince (ack/hata) bu lambda çalışır.
     *   Lambda farklı bir thread'de çalışır — callback thread-safe olmalı.
     *
     * RecordMetadata: Başarılı gönderimde broker bilgisi döner.
     *   topic: Hangi topic'e yazıldı.
     *   partition: Hangi partition'a düştü.
     *   offset: Partition içindeki pozisyon — idempotency kontrolü için kullanılabilir.
     *
     * Hata durumu → sendToDlq():
     *   Broker erişilemiyor veya timeout → hata loglanır, DLQ'ya gönderilir.
     *   DLQ'daki mesaj: Monitoring tarafından izlenir, gerekirse manuel tekrar denenebilir.
     */
    public CompletableFuture<SendResult<String, OrderEvent>> sendAsync(OrderEvent event) {
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(ordersTopic, event.getCustomerId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                RecordMetadata meta = result.getRecordMetadata();
                log.info("Sent: topic={}, partition={}, offset={}, orderId={}",
                        meta.topic(), meta.partition(), meta.offset(), event.getOrderId());
            } else {
                log.error("Send failed: {}, error: {}", event.getOrderId(), ex.getMessage());
                sendToDlq(event, ex.getMessage()); // Kayıp önleme: DLQ'ya yedekle
            }
        });
        return future;
    }

    /**
     * Sync (Blocking) — Güvenilir ama Yavaş.
     *
     * .get() ne yapar?
     *   CompletableFuture'ı senkron bekler — broker ack alana kadar thread bloke.
     *   Broker onaylamadan önce kod ilerlemez → kesin yazma garantisi.
     *
     * Ne zaman kullanılır?
     *   Ödeme onayı: "Ödeme event'i Kafka'ya yazıldı mı?" → ERP entegrasyon.
     *   Audit log: Her işlem kaydı kesin olmalı — kayıp kabul edilemez.
     *   Yasal gereklilik: Finansal işlemler — her adım izlenebilir olmalı.
     *
     * Performans maliyeti:
     *   Her send() bir network round-trip bekler.
     *   Throughput düşer: 10.000 mesaj/sn → 500-1000 mesaj/sn'ye düşebilir.
     *   Kural: Throughput önemliyse sync kullanma. Güvenilirlik öncelikliyse kullan.
     *
     * throws Exception: .get() InterruptedException ve ExecutionException fırlatır.
     *   ExecutionException: Kafka gönderim hatası — çağıran handle etmeli.
     */
    public RecordMetadata sendSync(OrderEvent event) throws Exception {
        SendResult<String, OrderEvent> result =
                kafkaTemplate.send(ordersTopic, event.getCustomerId(), event).get(); // Bloke eder
        RecordMetadata meta = result.getRecordMetadata();
        log.info("Sync sent: partition={}, offset={}", meta.partition(), meta.offset());
        return meta;
    }

    /**
     * Keyed Message — Partition Routing ile Sıra Garantisi.
     *
     * Kafka'da sıra garantisi nasıl çalışır?
     *   Global sıra: Yok — farklı partition'lar paralel işlenir.
     *   Partition bazlı sıra: Bir partition'daki mesajlar her zaman sıralı.
     *   Key kullanımı: Aynı key → aynı partition → o key için sıra garantisi.
     *
     * customerId key'i neden doğru seçim?
     *   Müşteri A: ORDER_PLACED (offset 5) → ORDER_CONFIRMED (offset 8) → ORDER_DELIVERED (offset 12)
     *   Aynı partition'da → consumer sırayla işler → durum makinesi doğru çalışır.
     *   Farklı partition'da olsaydı: DELIVERED önce, PLACED sonra gelebilir → tutarsız durum.
     *
     * Key seçimi dikkat:
     *   Kardinalite: customerId iyi (dağılım dengeli). orderId daha iyi (her sipariş ayrı key).
     *   Hot partition: Çok büyük müşteri tek key → bir partition aşırı yüklenir.
     *   Dengeli key: UUID veya hash'e göre dengeli dağılım.
     */
    public void sendKeyed(OrderEvent event) {
        // key = customerId → aynı müşterinin tüm event'leri aynı partition'da (sıra garantisi)
        kafkaTemplate.send(ordersTopic, event.getCustomerId(), event);
    }

    /**
     * Belirli Partition'a Gönderim — Manuel Partition Seçimi.
     *
     * Ne zaman manuel partition kullanılır?
     *   SLA ayrımı: Partition 0 = VIP müşteriler, Partition 1-N = standart.
     *   Dedicated consumer: VIP partition'ı ayrı consumer group ile öncelikli işler.
     *   Coğrafi ayrım: Partition 0 = İstanbul, Partition 1 = Ankara (bölgesel routing).
     *
     * ProducerRecord neden kullanılır?
     *   kafkaTemplate.send(topic, key, value): Kafka key hash → partition seçer.
     *   ProducerRecord(topic, partition, key, value): Partition'ı EXPLICIT belirt.
     *   Partition belirtilince key hash göz ardı edilir — manual override.
     *
     * Dikkat: Partition sayısı sabit değilse bu yaklaşım kırılgan olabilir.
     *   Topic yeniden yapılandırılırsa (partition artarsa) partition mapping bozulur.
     */
    public void sendToPartition(OrderEvent event, int partition) {
        ProducerRecord<String, OrderEvent> record = new ProducerRecord<>(
                ordersTopic,
                partition,              // Manuel partition seçimi
                event.getCustomerId(),  // Key (partition seçilmiş ama log için gerekli)
                event);
        kafkaTemplate.send(record);
        log.info("Sent to partition {}: {}", partition, event.getOrderId());
    }

    /**
     * Header ile Metadata Ekleme — Mesaj Zarfına Bilgi Ekle.
     *
     * Header vs Key vs Body farkı:
     *   Body (value): İş verisi — OrderEvent içeriği.
     *   Key: Partition routing için — customerId veya orderId.
     *   Header: Mesaj hakkında metadata — routing, tracing, kaynak bilgisi.
     *     Routing: Consumer "sadece source=payment-service header'ına sahip mesajları işle" diyebilir.
     *     Tracing: trace-id ile dağıtık sistemde istek takibi (distributed tracing).
     *     Monitoring: Hangi servis, hangi event tipi — Grafana dashboard'larında görünür.
     *
     * Neden binary (byte[]) format?
     *   Kafka header değerleri byte dizisi — string encode/decode gerektirir.
     *   StandardCharsets.UTF_8: Karakter seti tutarlı olmalı — encoding hatası önlenir.
     *   Consumer: new String(header.value(), StandardCharsets.UTF_8) ile okur.
     *
     * null timestamp: Kafka üretim zamanını otomatik ekler — explicit null vermek yeterli.
     */
    public void sendWithHeaders(OrderEvent event, String traceId, String source) {
        ProducerRecord<String, OrderEvent> record = new ProducerRecord<>(
                ordersTopic, null, event.getCustomerId(), event);
        record.headers()
                .add("trace-id", traceId.getBytes(StandardCharsets.UTF_8))   // Distributed tracing
                .add("source", source.getBytes(StandardCharsets.UTF_8))       // Hangi servis gönderdi
                .add("event-type", event.getEventType().getBytes(StandardCharsets.UTF_8)); // Hızlı filtreleme
        kafkaTemplate.send(record);
    }

    /**
     * Transactional Gönderim — Atomik Çok-Topic Yazma.
     *
     * Transactional producer neden gerekir?
     *   Senaryo: Sipariş event'i orders topic'e → Ödeme event'i payments topic'e.
     *   Birisi başarılıysa, diğeri değilse: Tutarsız veri — sipariş var ama ödeme yok.
     *   Transaction: İkisi ya birlikte commit'lenir ya da ikisi de iptal.
     *
     * Nasıl çalışır?
     *   executeInTransaction: Transaction başlatır.
     *   ops.send(): Her send() pending — henüz consumer'a görünmez.
     *   Lambda başarıyla döner → Kafka transaction COMMIT → tüm mesajlar consumer'a görünür.
     *   Exception fırlatılırsa → ABORT → hiçbiri görünmez.
     *   Consumer: isolation.level=read_committed → Sadece commit'lenmiş mesajları okur.
     *
     * Gereksinim: KafkaTemplate'in transactional producer'la konfigüre edilmesi.
     *   spring.kafka.producer.transaction-id-prefix: "tx-" ayarı zorunlu.
     *   Broker: Transaction log tutmak için kaynak kullanır — dikkatli kullan.
     */
    public void sendTransactional(OrderEvent orderEvent, OrderEvent paymentEvent) {
        kafkaTemplate.executeInTransaction(ops -> {
            ops.send(ordersTopic, orderEvent.getCustomerId(), orderEvent);
            ops.send(paymentsTopic, paymentEvent.getCustomerId(), paymentEvent);
            // İkisi ya birlikte commit → ya da ikisi abort (atomik)
            return true;
        });
        log.info("Transactional send: order={}, payment={}",
                orderEvent.getOrderId(), paymentEvent.getOrderId());
    }

    /**
     * DLQ'ya (Dead Letter Queue) Gönderim.
     *
     * Producer neden DLQ'ya yazar?
     *   Normalde consumer hata durumunda DLQ'ya yazar.
     *   Producer DLQ'su: Gönderim sırasında hata olursa (broker erişilemez) mesaj kaybolmaz.
     *   DLQ mesajı: Monitoring tarafından izlenir, sorun çözülünce replay edilir.
     *
     * dlq-reason header:
     *   İzlenebilirlik: "Neden DLQ'ya düştü?" sorusunun yanıtı.
     *   Alarm: DLQ'da mesaj artışı → operational alert tetiklenir.
     *   Replay: Sorun çözülünce DLQ'daki mesajlar orijinal topic'e geri taşınır.
     */
    public void sendToDlq(OrderEvent event, String reason) {
        ProducerRecord<String, OrderEvent> dlqRecord = new ProducerRecord<>(
                dlqTopic, event.getCustomerId(), event);
        dlqRecord.headers().add("dlq-reason", reason.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(dlqRecord);
        log.warn("DLQ: {} — reason: {}", event.getOrderId(), reason);
    }

    /**
     * Batch Gönderim — Toplu Mesaj.
     *
     * Dikkat: Bu "gerçek" Kafka batch değil.
     *   forEach: Her event için ayrı send() → N ayrı mesaj.
     *   Kafka producer: Kendi iç buffer'ında mesajları biriktirip batch gönderir (linger.ms).
     *   Bu metod: Uygulama seviyesinde loop — Kafka'nın kendi batch mekanizması ayrı.
     *
     * Kafka producer batch mekanizması (arka planda):
     *   linger.ms: Producer N ms bekler, buffer dolana kadar mesajları biriktir.
     *   batch.size: Buffer dolunca tek TCP paketinde gönder.
     *   Bu ayarlar KafkaProducerConfig'de yapılandırılır — uygulama koduna dokunmak gerekmez.
     */
    public void sendBatch(List<OrderEvent> events) {
        events.forEach(this::sendFireAndForget);
        log.info("Batch sent: {} events", events.size());
    }
}
