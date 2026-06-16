package com.autohub.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Araç Güncelleme İsteği DTO
 *
 * Tüm alanlar opsiyoneldir - sadece gönderilen alanlar güncellenir (PATCH davranışı).
 * Bu pattern "Partial Update" olarak bilinir ve gereksiz veri transferini önler.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleUpdateRequest {

    /**
     * Yeni plaka numarası - değiştirilmek istenirse gönderilir
     */
    @Pattern(regexp = "^[0-9]{2}\\s[A-Z]{1,3}\\s[0-9]{2,4}$",
             message = "Geçersiz plaka formatı")
    private String licensePlate;

    /**
     * Yeni renk - değiştirilmek istenirse gönderilir
     */
    @Size(max = 50)
    private String color;

    /**
     * Güncel kilometre değeri
     */
    @PositiveOrZero(message = "Kilometre negatif olamaz")
    private Integer mileage;

    /**
     * Yeni fiyat
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Fiyat sıfırdan büyük olmalıdır")
    private BigDecimal price;

    /**
     * Yeni günlük kiralama ücreti
     */
    @DecimalMin(value = "0.0")
    private BigDecimal dailyRate;

    /**
     * Güncellenmiş açıklama
     */
    @Size(max = 2000)
    private String description;

    /**
     * Güncellenmiş fotoğraf listesi
     */
    private List<String> imageUrls;

    /**
     * Son muayene tarihi güncelleme
     */
    private LocalDate lastInspectionDate;

    /**
     * Sigorta bitiş tarihi güncelleme
     */
    private LocalDate insuranceExpiryDate;

    /**
     * Güncellenmiş özellik listesi
     */
    private List<String> features;
}
