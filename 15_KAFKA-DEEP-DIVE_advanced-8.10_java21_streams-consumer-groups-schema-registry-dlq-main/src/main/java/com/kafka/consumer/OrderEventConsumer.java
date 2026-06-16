package com.kafka.consumer;

import com.kafka.model.OrderEvent;
import com.kafka.producer.OrderEventProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Kafka Consumer Desenleri:
 *   - Manuel commit (MANUAL_IMMEDIATE)
 *   - Batch consumer
 *   - Specific partition consumer
 *   - Header okuma
 *   - Error handling + DLQ
 *   - Consumer group — offset yönetimi
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final int MAX_RETRIES = 3;

    private final OrderEventProducer producer;

    public OrderEventConsumer(OrderEventProducer producer) {
        this.producer = producer;
    }

    /**
     * Temel Consumer — Manuel Offset Commit (At-Least-Once Delivery)
     *
     * @KafkaListener parametreleri:
     *   topics       → dinlenecek topic adı (application.yml'dan)
     *   groupId      → consumer group ID — aynı group içindeki consumer'lar
     *                   partition'ları paylaşır (yük dağılımı)
     *   containerFactory → hangi KafkaListenerContainerFactory bean'i kullanılacak
     *                       (ack mode, batch/single, error handler ayarları burada)
     *
     * Manuel commit (MANUAL_IMMEDIATE ack mode):
     *   - enable-auto-commit: false → Kafka offset'i otomatik commit etmez
     *   - ack.acknowledge() → offset'i şimdi commit et (mesaj işlendi)
     *   - ack.nack(delay) → negatif ack: N saniye sonra yeniden teslim et
     *
     * At-least-once garantisi:
     *   Consumer crash ederse uncommitted offset tekrar başlar → mesaj yeniden gelir
     *   Bu yüzden iş mantığı idempotent olmalı (aynı orderId iki kez işlense de sorun yok)
     */
    @KafkaListener(
            topics = "${app.topics.orders}",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrder(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received: partition={}, offset={}, orderId={}", partition, offset, event.getOrderId());

        try {
            handleOrderEvent(event);
            ack.acknowledge(); // offset commit → "bu mesajı işledim, bir sonrakine geç"
        } catch (Exception e) {
            log.error("Processing failed: {}", e.getMessage());
            // nack: offset commit etme, 5 saniye sonra yeniden teslim et
            // Dikkat: Sonsuz retry döngüsü riski — DLQ stratejisi ile sınırlandır
            ack.nack(Duration.ofSeconds(5));
        }
    }

    private void handleOrderEvent(OrderEvent event) {
        switch (event.getEventType()) {
            case "ORDER_PLACED"     -> log.info("New order: {}", event.getOrderId());
            case "ORDER_CONFIRMED"  -> log.info("Order confirmed: {}", event.getOrderId());
            case "ORDER_CANCELLED"  -> log.info("Order cancelled: {}", event.getOrderId());
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    /**
     * Batch Consumer — Toplu İşlem (Yüksek Throughput)
     *
     * batchKafkaListenerContainerFactory: batchListener=true olarak yapılandırılmış.
     * Tek metod çağrısında N mesaj → toplu DB insert, toplu HTTP call.
     *
     * Avantaj: N×1 yerine 1 DB round-trip → throughput artar.
     * Dezavantaj: Bir mesaj başarısız olursa tüm batch retry gerekebilir.
     * Çözüm: Başarısız mesajları ayrıştır, sadece onları DLQ'ya gönder.
     */
    @KafkaListener(
            topics = "${app.topics.orders}",
            groupId = "batch-group",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void processBatch(List<ConsumerRecord<String, OrderEvent>> records, Acknowledgment ack) {
        log.info("Batch received: {} records", records.size());
        records.forEach(record ->
                log.info("  partition={}, offset={}, key={}",
                        record.partition(), record.offset(), record.key()));
        // toplu DB insert, API call vs. burada — single transaction
        ack.acknowledge(); // tüm batch başarılı → tek commit
    }

    /**
     * Specific Partition Consumer — Manuel Partition Ataması
     *
     * Normal @KafkaListener: Consumer group içinde partition'lar otomatik dağıtılır.
     * topicPartitions: "Ben sadece partition 0'ı dinleyeyim" — VIP müşteri trafiği
     * ayrı bir partition'da olsun, dedicated consumer ile öncelikli işlensin.
     *
     * Kullanım: SLA gereksinimleri farklı iş kolları — premium müşteriler partition 0,
     * standart müşteriler partition 1-N. Consumer grubu farklı (vip-group) olduğu için
     * rebalance'dan bağımsız.
     */
    @KafkaListener(
            topicPartitions = @TopicPartition(
                    topic = "${app.topics.orders}",
                    partitions = {"0"}     // sadece partition 0 — VIP trafiği buraya key'lendi
            ),
            groupId = "vip-group"
    )
    public void processVipOrders(OrderEvent event, Acknowledgment ack) {
        log.info("VIP partition-0 consumer: {}", event.getOrderId());
        ack.acknowledge();
    }

    // Header okuma (traceId, source)
    @KafkaListener(
            topics = "${app.topics.orders}",
            groupId = "audit-group"
    )
    public void auditConsumer(ConsumerRecord<String, OrderEvent> record) {
        String traceId = extractHeader(record, "trace-id");
        String source = extractHeader(record, "source");
        log.info("AUDIT — traceId: {}, source: {}, orderId: {}",
                traceId, source, record.value().getOrderId());
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : "unknown";
    }

    // DLQ consumer — başarısız mesajları işle veya alarm ver
    @KafkaListener(
            topics = "${app.topics.dlq}",
            groupId = "dlq-handler-group"
    )
    public void handleDlq(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
        Header reasonHeader = record.headers().lastHeader("dlq-reason");
        String reason = reasonHeader != null
                ? new String(reasonHeader.value(), StandardCharsets.UTF_8) : "unknown";

        log.error("DLQ message — orderId: {}, reason: {}",
                record.value().getOrderId(), reason);

        // Alarm gönder / Slack notify / DB'ye kaydet
        ack.acknowledge();
    }

    // Payment consumer (farklı topic, farklı group)
    @KafkaListener(
            topics = "${app.topics.payments}",
            groupId = "payment-service-group"
    )
    public void processPayment(OrderEvent event, Acknowledgment ack) {
        log.info("Payment event: orderId={}, amount={}", event.getOrderId(), event.getAmount());
        ack.acknowledge();
    }
}
