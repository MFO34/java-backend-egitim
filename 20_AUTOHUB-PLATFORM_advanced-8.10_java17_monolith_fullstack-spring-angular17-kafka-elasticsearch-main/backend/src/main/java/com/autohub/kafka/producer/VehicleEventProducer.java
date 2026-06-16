package com.autohub.kafka.producer;

// Kafka ve proje importları
import com.autohub.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Araç Event Üreticisi - Kafka'ya mesaj gönderir
 *
 * Apache Kafka kullanımı - Asenkron Mesajlaşma:
 * Araç oluşturma, güncelleme ve silme olayları Kafka topic'lerine gönderilir.
 * Bu sayede diğer servisler (bildirim servisi, arama indexi vs.) olayları tüketebilir.
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * Kafka ile iletişim bu sınıfta kapsüllenir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleEventProducer {

    // Kafka mesaj gönderme template'i - Spring tarafından enjekte edilir
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Kafka topic adları - sabit değerler
    private static final String VEHICLE_CREATED_TOPIC = "vehicle-created";
    private static final String VEHICLE_UPDATED_TOPIC = "vehicle-updated";
    private static final String VEHICLE_DELETED_TOPIC = "vehicle-deleted";
    private static final String VEHICLE_STATUS_CHANGED_TOPIC = "vehicle-status-changed";

    /**
     * Araç oluşturma event'i gönderir
     * Yeni araç eklendiğinde Elasticsearch indexini güncelleme, bildirim gönderme vs.
     * için kullanılır
     *
     * @param vehicle Oluşturulan araç entity'si
     */
    public void sendVehicleCreatedEvent(Vehicle vehicle) {
        // Event payload oluştur
        VehicleEvent event = new VehicleEvent(
            vehicle.getId(),
            vehicle.getLicensePlate(),
            vehicle.getStatus().name(),
            "CREATED"
        );

        // Kafka'ya asenkron gönder - callback ile sonucu takip et
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(VEHICLE_CREATED_TOPIC, vehicle.getId().toString(), event);

        // Gönderim sonucunu logla (başarı veya hata)
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Başarılı gönderim
                log.info("Araç oluşturma event'i gönderildi: vehicleId={}, topic={}, partition={}",
                    vehicle.getId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition());
            } else {
                // Gönderim başarısız - log yaz ama uygulamayı durdurma
                log.error("Araç oluşturma event'i gönderilemedi: vehicleId={}, hata: {}",
                    vehicle.getId(), ex.getMessage());
            }
        });
    }

    /**
     * Araç güncelleme event'i gönderir
     *
     * @param vehicle Güncellenen araç
     */
    public void sendVehicleUpdatedEvent(Vehicle vehicle) {
        VehicleEvent event = new VehicleEvent(
            vehicle.getId(),
            vehicle.getLicensePlate(),
            vehicle.getStatus().name(),
            "UPDATED"
        );

        // Fire and forget - yanıt bekleme (asenkron)
        kafkaTemplate.send(VEHICLE_UPDATED_TOPIC, vehicle.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Araç güncelleme event'i gönderilemedi: {}", ex.getMessage());
                }
            });
    }

    /**
     * Araç durum değişikliği event'i gönderir
     * Kiralama/satış işlemlerinde araç durumu değiştiğinde bildirim için
     *
     * @param vehicleId Araç ID'si
     * @param oldStatus Eski durum
     * @param newStatus Yeni durum
     */
    public void sendVehicleStatusChangedEvent(Long vehicleId, String oldStatus, String newStatus) {
        // Durum değişikliği event'i oluştur
        VehicleStatusChangeEvent event = new VehicleStatusChangeEvent(vehicleId, oldStatus, newStatus);

        kafkaTemplate.send(VEHICLE_STATUS_CHANGED_TOPIC, vehicleId.toString(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Araç durum değişikliği event'i gönderildi: vehicleId={}, {} -> {}",
                        vehicleId, oldStatus, newStatus);
                } else {
                    log.error("Araç durum değişikliği event'i gönderilemedi: {}", ex.getMessage());
                }
            });
    }

    /**
     * Araç event payload sınıfı - Kafka mesaj içeriği
     * record: Değişmez (immutable) veri sınıfı - Java 16+
     */
    public record VehicleEvent(
        Long vehicleId,          // Araç ID'si
        String licensePlate,     // Plaka numarası
        String status,           // Araç durumu
        String eventType         // Olay tipi: CREATED, UPDATED, DELETED
    ) {}

    /**
     * Araç durum değişikliği event payload sınıfı
     */
    public record VehicleStatusChangeEvent(
        Long vehicleId,
        String oldStatus,
        String newStatus
    ) {}
}
