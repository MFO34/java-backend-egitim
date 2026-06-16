package com.autohub.entity;

// JPA, doğrulama ve Lombok importları
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Araç Modeli Entity Sınıfı
 *
 * Bir marka birden fazla modele sahip olabilir (Toyota -> Corolla, Camry, RAV4 vs.)
 * Bu sınıf araç modellerini temsil eder ve Brand ile Many-to-One ilişkisi kurar.
 *
 * OOP Prensibi - KALITIM (Inheritance):
 * BaseEntity'den miras alarak ortak alanlar ve davranışlar elde edilir.
 *
 * OOP Prensibi - İLİŞKİLENDİRME (Association):
 * Brand ile Many-to-One ilişki - çok model bir markaya ait olabilir
 * Vehicle ile One-to-Many ilişki - bir modelin birden fazla aracı olabilir
 */
@Entity
@Table(name = "vehicle_models",
        indexes = {
            // Marka ve model adı kombinasyonu benzersiz olmalı
            @Index(name = "idx_model_brand_name", columnList = "brand_id, name", unique = true),
            // Model yılına göre sorgulamaları hızlandırmak için
            @Index(name = "idx_model_year", columnList = "model_year"),
            // Model tipi bazlı filtreleme için
            @Index(name = "idx_model_type", columnList = "vehicle_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"brand", "vehicles"})  // Sonsuz döngüyü önlemek için
public class VehicleModel extends BaseEntity {

    /**
     * Araç markası ile Many-to-One ilişkisi
     * Çok model bir markaya ait olabilir
     * FetchType.LAZY: Marka bilgisi sadece ihtiyaç duyulduğunda yüklenir (N+1 problemini önler)
     */
    @NotNull(message = "Marka bilgisi zorunludur")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_model_brand"))  // Foreign key adını belirt
    private Brand brand;

    /**
     * Model adı
     * Örnek: "Corolla", "Civic", "Golf"
     */
    @NotBlank(message = "Model adı boş olamaz")
    @Size(min = 1, max = 100, message = "Model adı 1-100 karakter arasında olmalıdır")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Araç tipi - Enum kullanımı
     * Örnek: SEDAN, HATCHBACK, SUV, PICKUP, COUPE, CONVERTIBLE
     */
    @NotNull(message = "Araç tipi zorunludur")
    @Enumerated(EnumType.STRING)  // Veritabanında string olarak sakla (sayı değil)
    @Column(name = "vehicle_type", nullable = false, length = 50)
    private VehicleType vehicleType;

    /**
     * Model yılı
     * Örnek: 2023
     */
    @Positive(message = "Model yılı pozitif bir sayı olmalıdır")
    @Column(name = "model_year")
    private Integer modelYear;

    /**
     * Motor hacmi (cc cinsinden)
     * Örnek: 1600 (1.6 litre motor)
     */
    @Column(name = "engine_capacity")
    private Integer engineCapacity;

    /**
     * Yakıt tipi
     * Örnek: GASOLINE, DIESEL, ELECTRIC, HYBRID
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 30)
    private FuelType fuelType;

    /**
     * Vites tipi
     * Örnek: MANUAL, AUTOMATIC, SEMI_AUTOMATIC
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_type", length = 30)
    private TransmissionType transmissionType;

    /**
     * Koltuk kapasitesi
     */
    @Column(name = "seat_count")
    private Integer seatCount;

    /**
     * Kılavuz belge URL'i
     */
    @Column(name = "manual_url", length = 500)
    private String manualUrl;

    /**
     * Bu modele ait araçlar - One-to-Many ilişkisi
     * Bir modelin birden fazla aracı olabilir (aynı modelden birden fazla stok)
     */
    @OneToMany(mappedBy = "model",
               cascade = {CascadeType.PERSIST, CascadeType.MERGE},  // Silme cascade'i yok
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();

    /**
     * Araç tipi enum sınıfı
     * Tüm desteklenen araç tiplerini listeler
     */
    public enum VehicleType {
        SEDAN,        // Sedan
        HATCHBACK,    // Hatchback
        SUV,          // Sport Utility Vehicle
        PICKUP,       // Kamyonet
        COUPE,        // Coupe
        CONVERTIBLE,  // Cabrio
        MINIVAN,      // Minivan
        TRUCK,        // Kamyon
        MOTORCYCLE,   // Motosiklet
        OTHER         // Diğer
    }

    /**
     * Yakıt tipi enum sınıfı
     */
    public enum FuelType {
        GASOLINE,     // Benzin
        DIESEL,       // Dizel
        ELECTRIC,     // Elektrikli
        HYBRID,       // Hibrit
        LPG,          // LPG
        NATURAL_GAS   // Doğalgaz
    }

    /**
     * Vites tipi enum sınıfı
     */
    public enum TransmissionType {
        MANUAL,          // Manuel
        AUTOMATIC,       // Otomatik
        SEMI_AUTOMATIC   // Yarı otomatik (CVT, DSG vs.)
    }
}
