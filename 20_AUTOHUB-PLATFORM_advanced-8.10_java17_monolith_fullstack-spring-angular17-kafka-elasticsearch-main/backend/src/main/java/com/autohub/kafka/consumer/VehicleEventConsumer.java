package com.autohub.kafka.consumer;

// Kafka ve proje importları
import com.autohub.kafka.producer.VehicleEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Araç Event Tüketicisi - Kafka topic'lerinden mesaj dinler
 *
 * Kafka Consumer olarak çalışır ve araç olaylarını işler.
 * Elasticsearch indexi güncelleme, bildirim gönderme gibi işlemler burada yapılır.
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * Kafka consumer işlemleri bu sınıfta izole edilmiştir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleEventConsumer {

    /**
     * Araç oluşturma olaylarını dinler
     * Elasticsearch'e yeni araç ekler, bildirim gönderir
     *
     * @KafkaListener: Belirtilen topic'i dinler
     * groupId: Tüketici grubu - aynı gruptaki consumer'lar yükü paylaşır
     *
     * @param event Kafka'dan gelen araç event payload'ı
     * @param partition Mesajın geldiği partition numarası
     * @param offset Partition içindeki mesaj pozisyonu
     */
    @KafkaListener(
        topics = "vehicle-created",
        groupId = "autohub-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVehicleCreatedEvent(
            @Payload VehicleEventProducer.VehicleEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        // Gelen mesajı logla
        log.info("Araç oluşturma event'i alındı: vehicleId={}, partition={}, offset={}",
            event.vehicleId(), partition, offset);

        try {
            // Elasticsearch'e yeni araç indexi ekle
            // (Gerçek implementasyonda ElasticsearchRepository çağrılır)
            log.info("Elasticsearch indexi güncelleniyor: vehicleId={}", event.vehicleId());

            // Bildirim servisi çağrısı (asenkron)
            log.info("Yöneticilere bildirim gönderiliyor: Yeni araç eklendi, ID={}", event.vehicleId());

            // İşlem başarılı
            log.info("Araç oluşturma event'i başarıyla işlendi: vehicleId={}", event.vehicleId());

        } catch (Exception e) {
            // Hata durumunda loglama - Dead Letter Queue'ya gönderilebilir
            log.error("Araç oluşturma event'i işlenirken hata: vehicleId={}, hata={}",
                event.vehicleId(), e.getMessage(), e);
            // Hata yönetimi: Retry mekanizması veya DLQ (Dead Letter Queue) kullanılabilir
        }
    }

    /**
     * Araç güncelleme olaylarını dinler
     * Elasticsearch'teki araç bilgilerini günceller
     */
    @KafkaListener(
        topics = "vehicle-updated",
        groupId = "autohub-consumer-group"
    )
    public void consumeVehicleUpdatedEvent(@Payload VehicleEventProducer.VehicleEvent event) {
        log.info("Araç güncelleme event'i alındı: vehicleId={}", event.vehicleId());

        // Elasticsearch indexini güncelle
        log.info("Elasticsearch indexi güncelleniyor (güncelleme): vehicleId={}", event.vehicleId());
    }

    /**
     * Araç durum değişikliği olaylarını dinler
     * Örneğin araç kiralandığında müşteriye SMS/e-posta gönder
     */
    @KafkaListener(
        topics = "vehicle-status-changed",
        groupId = "autohub-consumer-group"
    )
    public void consumeVehicleStatusChangedEvent(
            @Payload VehicleEventProducer.VehicleStatusChangeEvent event) {
        log.info("Araç durum değişikliği: vehicleId={}, {} -> {}",
            event.vehicleId(), event.oldStatus(), event.newStatus());

        // Durum değişikliğine göre iş mantığı uygula
        switch (event.newStatus()) {
            case "RENTED" ->
                // Araç kiralandı - müşteriye bildirim gönder
                log.info("Kiralama bildirimi gönderiliyor: vehicleId={}", event.vehicleId());
            case "SOLD" ->
                // Araç satıldı - satış raporu oluştur
                log.info("Satış raporu oluşturuluyor: vehicleId={}", event.vehicleId());
            case "MAINTENANCE" ->
                // Bakıma alındı - bakım bildirimi
                log.info("Bakım bildirimi gönderiliyor: vehicleId={}", event.vehicleId());
            default ->
                log.debug("İşlenmemiş durum değişikliği: {}", event.newStatus());
        }
    }
}
