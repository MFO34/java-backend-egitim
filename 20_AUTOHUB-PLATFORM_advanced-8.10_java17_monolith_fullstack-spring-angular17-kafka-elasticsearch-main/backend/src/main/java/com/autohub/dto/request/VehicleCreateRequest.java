package com.autohub.dto.request;

// Doğrulama ve Lombok importları
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Araç Oluşturma İsteği DTO (Data Transfer Object)
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Gelen HTTP isteği verilerini güvenli bir şekilde kapsüller.
 * Entity'e doğrudan maruz kalmak yerine DTO kullanılır.
 *
 * DTO Kullanım Amacı:
 * 1. Güvenlik: Sadece izin verilen alanlar alınır
 * 2. Doğrulama: @Valid anotasyonu ile otomatik validation
 * 3. Ayrıştırma: API contract ile iç model ayrı tutulur
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleCreateRequest {

    /**
     * Araç model ID'si - hangi modele ait olduğunu belirtir
     */
    @NotNull(message = "Model ID zorunludur")
    private Long modelId;

    /**
     * Araç plaka numarası - Türkiye formatında
     */
    @NotBlank(message = "Plaka numarası zorunludur")
    @Pattern(regexp = "^[0-9]{2}\\s[A-Z]{1,3}\\s[0-9]{2,4}$",
             message = "Geçersiz plaka formatı. Örnek: 34 ABC 1234")
    private String licensePlate;

    /**
     * Üretim yılı
     */
    @NotNull(message = "Üretim yılı zorunludur")
    @Min(value = 1886, message = "Üretim yılı 1886'dan küçük olamaz")
    @Max(value = 2030, message = "Geçersiz yıl")
    private Integer year;

    /**
     * Araç rengi
     */
    @Size(max = 50, message = "Renk adı 50 karakterden fazla olamaz")
    private String color;

    /**
     * Mevcut kilometre
     */
    @PositiveOrZero(message = "Kilometre negatif olamaz")
    private Integer mileage;

    /**
     * Satış fiyatı
     */
    @NotNull(message = "Fiyat zorunludur")
    @DecimalMin(value = "0.0", inclusive = false, message = "Fiyat sıfırdan büyük olmalıdır")
    private BigDecimal price;

    /**
     * Günlük kiralama ücreti
     */
    @DecimalMin(value = "0.0", message = "Günlük kiralama ücreti negatif olamaz")
    private BigDecimal dailyRate;

    /**
     * 17 haneli VIN numarası (opsiyonel)
     */
    @Size(min = 17, max = 17, message = "VIN numarası 17 karakter olmalıdır")
    private String vinNumber;

    /**
     * Motor seri numarası (opsiyonel)
     */
    @Size(max = 50)
    private String engineNumber;

    /**
     * Araç açıklaması
     */
    @Size(max = 2000, message = "Açıklama 2000 karakterden fazla olamaz")
    private String description;

    /**
     * Araç fotoğrafları URL listesi
     */
    private List<String> imageUrls;

    /**
     * Son muayene tarihi
     */
    private LocalDate lastInspectionDate;

    /**
     * Sigorta bitiş tarihi
     */
    private LocalDate insuranceExpiryDate;

    /**
     * Araç özellikleri listesi (sunroof, deri koltuk vs.)
     */
    private List<String> features;
}
