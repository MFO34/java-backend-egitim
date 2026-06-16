package com.autohub.entity;

// JPA, Lombok, Elasticsearch ve doğrulama importları
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Araç Entity Sınıfı - AutoHub sisteminin temel varlığı
 *
 * OOP Prensibi - KALITIM (Inheritance):
 * BaseEntity'den miras alarak ortak alanları elde eder.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Araç durumu (status) gibi hassas alanlar private tutulur ve iş kuralları
 * içeren setter metotları ile kontrol edilir.
 *
 * OOP Prensibi - POLİMORFİZM (Polymorphism):
 * VehicleStatus enum'u ile araç durumu çok biçimli davranış gösterir.
 *
 * @Document: Elasticsearch indeksi konfigürasyonu - full-text arama için
 */
@Entity
@Table(name = "vehicles",
        indexes = {
            // Plakaya göre benzersiz index - aynı plakada iki araç olamaz
            @Index(name = "idx_vehicle_plate", columnList = "license_plate", unique = true),
            // Araç durumuna göre filtreleme için
            @Index(name = "idx_vehicle_status", columnList = "status"),
            // Modele ve yıla göre arama için bileşik index
            @Index(name = "idx_vehicle_model_year", columnList = "model_id, year"),
            // Fiyat bazlı sorgulamaları hızlandırmak için
            @Index(name = "idx_vehicle_price", columnList = "price")
        })
@Document(indexName = "vehicles")  // Elasticsearch index adı
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"model", "transactions"})
public class Vehicle extends BaseEntity {

    /**
     * Araç modeli ile Many-to-One ilişkisi
     * Çok araç aynı modele ait olabilir
     */
    @NotNull(message = "Model bilgisi zorunludur")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_vehicle_model"))
    private VehicleModel model;

    /**
     * Araç plakası - benzersiz olmalı
     * Türk plaka formatına uygun olmalı: 34 ABC 123
     */
    @NotBlank(message = "Plaka numarası zorunludur")
    @Pattern(regexp = "^[0-9]{2}\\s[A-Z]{1,3}\\s[0-9]{2,4}$",
             message = "Geçersiz plaka formatı. Örnek: 34 ABC 1234")
    @Column(name = "license_plate", nullable = false, unique = true, length = 15)
    @Field(type = FieldType.Keyword)  // Elasticsearch'te tam eşleşme için
    private String licensePlate;

    /**
     * Araç üretim yılı
     */
    @NotNull(message = "Üretim yılı zorunludur")
    @Min(value = 1886, message = "Üretim yılı 1886'dan küçük olamaz")  // İlk araç 1886
    @Max(value = 2030, message = "Üretim yılı geçersiz")
    @Column(name = "year", nullable = false)
    private Integer year;

    /**
     * Araç rengi
     * Örnek: "Beyaz", "Siyah", "Gümüş"
     */
    @Size(max = 50, message = "Renk adı 50 karakterden fazla olamaz")
    @Column(name = "color", length = 50)
    @Field(type = FieldType.Text)
    private String color;

    /**
     * Kilometre sayacı değeri
     */
    @PositiveOrZero(message = "Kilometre değeri negatif olamaz")
    @Column(name = "mileage")
    private Integer mileage;

    /**
     * Araç satış/kiralama fiyatı
     * BigDecimal kullanımı: Parasal değerlerde kayan nokta hataları önlenir
     */
    @NotNull(message = "Fiyat bilgisi zorunludur")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Günlük kiralama ücreti (kiralamalar için)
     */
    @DecimalMin(value = "0.0", message = "Günlük kira ücreti negatif olamaz")
    @Column(name = "daily_rate", precision = 10, scale = 2)
    private BigDecimal dailyRate;

    /**
     * Araç VIN (Vehicle Identification Number) numarası
     * 17 haneli uluslararası araç kimlik numarası
     */
    @Size(min = 17, max = 17, message = "VIN numarası 17 karakter olmalıdır")
    @Column(name = "vin_number", unique = true, length = 17)
    private String vinNumber;

    /**
     * Motor seri numarası
     */
    @Column(name = "engine_number", unique = true, length = 50)
    private String engineNumber;

    /**
     * Araç açıklaması - full-text arama için Elasticsearch'te indexlenir
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Field(type = FieldType.Text, analyzer = "turkish")  // Türkçe dil analizi
    private String description;

    /**
     * Araç fotoğrafları - URL listesi JSON olarak saklanır
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vehicle_images",
                     joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    /**
     * Araç durumu - duruma göre farklı işlemler mümkündür (Polimorfizm)
     */
    @NotNull(message = "Araç durumu zorunludur")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VehicleStatus status = VehicleStatus.AVAILABLE;  // Varsayılan: Müsait

    /**
     * Son muayene tarihi
     */
    @Column(name = "last_inspection_date")
    private LocalDate lastInspectionDate;

    /**
     * Sigorta bitiş tarihi
     */
    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    /**
     * Araç özellik etiketleri (sunroof, deri koltuk vs.)
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vehicle_features",
                     joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "feature", length = 100)
    private List<String> features = new ArrayList<>();

    /**
     * Bu araca ait işlemler (satış/kiralama)
     */
    @OneToMany(mappedBy = "vehicle",
               cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    /**
     * Araç müsait mi kontrolü
     * İş mantığı entity içinde tutulur - Domain Driven Design prensibi
     *
     * @return true ise araç müsait, kiralama/satış yapılabilir
     */
    public boolean isAvailable() {
        // Araç aktif ve müsait durumunda ise kullanılabilir
        return this.isActive && VehicleStatus.AVAILABLE.equals(this.status);
    }

    /**
     * Araç durumunu günceller - iş kuralları ile birlikte
     * Kapsülleme: Durum değişikliği doğrudan değil, bu metot ile yapılır
     *
     * @param newStatus Yeni araç durumu
     * @throws IllegalStateException Geçersiz durum geçişinde fırlatılır
     */
    public void updateStatus(VehicleStatus newStatus) {
        // Satılmış bir araç tekrar müsait yapılamaz
        if (VehicleStatus.SOLD.equals(this.status) && !VehicleStatus.SOLD.equals(newStatus)) {
            throw new IllegalStateException("Satılmış araç durumu değiştirilemez: " + this.licensePlate);
        }
        // Durum güncelleniyor
        this.status = newStatus;
    }

    /**
     * Araç durumu enum sınıfı
     * Tüm olası araç durumlarını tanımlar
     */
    public enum VehicleStatus {
        AVAILABLE,    // Müsait - kiralama veya satışa hazır
        RENTED,       // Kiralanmış - aktif kiralama var
        SOLD,         // Satılmış - artık satışta değil
        MAINTENANCE,  // Bakımda - geçici olarak kullanılamaz
        RESERVED,     // Rezerve - ön rezervasyon yapılmış
        DAMAGED       // Hasarlı - onarım gerektiriyor
    }
}
