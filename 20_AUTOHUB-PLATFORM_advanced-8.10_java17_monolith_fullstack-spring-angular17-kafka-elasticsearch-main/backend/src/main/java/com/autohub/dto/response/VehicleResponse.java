package com.autohub.dto.response;

import com.autohub.entity.Vehicle;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Araç Yanıt DTO - API'den dönen araç verisi yapısı
 *
 * Client'a hangi alanların gönderileceğini bu sınıf belirler.
 * Entity alanlarından farklı olarak hesaplanmış alanlar da eklenebilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    /** Araç ID'si */
    private Long id;

    /** Plaka numarası */
    private String licensePlate;

    /** Üretim yılı */
    private Integer year;

    /** Renk */
    private String color;

    /** Kilometre */
    private Integer mileage;

    /** Fiyat */
    private BigDecimal price;

    /** Günlük kiralama ücreti */
    private BigDecimal dailyRate;

    /** VIN numarası */
    private String vinNumber;

    /** Açıklama */
    private String description;

    /** Araç durumu */
    private Vehicle.VehicleStatus status;

    /** Model ID'si */
    private Long modelId;

    /** Model adı */
    private String modelName;

    /** Marka adı */
    private String brandName;

    /** Fotoğraf URL'leri */
    private List<String> imageUrls;

    /** Son muayene tarihi */
    private LocalDate lastInspectionDate;

    /** Sigorta bitiş tarihi */
    private LocalDate insuranceExpiryDate;

    /** Oluşturulma tarihi */
    private LocalDateTime createdAt;

    /** Son güncelleme tarihi */
    private LocalDateTime updatedAt;

    /** Araç müsait mi - hesaplanan alan */
    public boolean isAvailable() {
        return Vehicle.VehicleStatus.AVAILABLE.equals(status);
    }
}
