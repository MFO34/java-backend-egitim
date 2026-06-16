package com.autohub.entity;

// JPA, Lombok ve validation importları
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Araç Markası Entity Sınıfı
 *
 * OOP Prensibi - KALITIM (Inheritance):
 * BaseEntity'den miras alarak id, createdAt, updatedAt gibi ortak alanları miras alır.
 * Bu sayede her entity sınıfında bu alanları tekrar tanımlamak zorunda kalmayız.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Tüm alanlar private olarak tanımlanmış, erişim sadece getter/setter ile sağlanır.
 * Lombok @Getter ve @Setter anotasyonları bu metotları otomatik oluşturur.
 *
 * @Entity: Bu sınıfın bir JPA entity'si olduğunu belirtir - veritabanında tablosu oluşur
 * @Table: Veritabanında "brands" tablosuna karşılık gelir
 */
@Entity
@Table(name = "brands",
        indexes = {
            // Marka adı üzerinde benzersiz index - aynı isimde iki marka olamaz
            @Index(name = "idx_brand_name", columnList = "name", unique = true),
            // Ülke bazlı sorgulamaları hızlandırmak için index
            @Index(name = "idx_brand_country", columnList = "country")
        })
@Getter                    // Lombok: Tüm alanlar için getter metodları oluşturur
@Setter                    // Lombok: Tüm alanlar için setter metodları oluşturur
@NoArgsConstructor         // Lombok: Parametresiz constructor oluşturur (JPA için gerekli)
@AllArgsConstructor        // Lombok: Tüm alanları alan constructor oluşturur
@Builder                   // Lombok: Builder pattern implementasyonu sağlar
@ToString(exclude = "models")  // Sonsuz döngüyü önlemek için models listesini toString'den hariç tut
public class Brand extends BaseEntity {

    /**
     * Marka adı - zorunlu alan, benzersiz olmalı
     * Örnek: "Toyota", "BMW", "Mercedes-Benz"
     */
    @NotBlank(message = "Marka adı boş olamaz")
    @Size(min = 2, max = 100, message = "Marka adı 2-100 karakter arasında olmalıdır")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Markanın kurulduğu ülke
     * Örnek: "Japonya", "Almanya", "Amerika Birleşik Devletleri"
     */
    @Size(max = 100, message = "Ülke adı 100 karakterden fazla olamaz")
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Marka logosu URL adresi
     * Örnek: "https://cdn.autohub.com/logos/toyota.png"
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * Markanın kuruluş yılı
     * Örnek: Toyota 1937 yılında kurulmuştur
     */
    @Column(name = "founded_year")
    private Integer foundedYear;

    /**
     * Marka hakkında kısa açıklama
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Markaya ait araç modelleri - One-to-Many ilişki
     *
     * OOP Prensibi - KAPSÜLLEME:
     * Modeller listesi Brand sınıfı içinde kapsüllenmiştir.
     * Cascade işlemleri: Marka silindiğinde modeller de silinir (CascadeType.ALL)
     * Lazy loading: Modeller sadece erişildiğinde veritabanından yüklenir (performans için)
     *
     * mappedBy = "brand": VehicleModel sınıfındaki "brand" alanı bu ilişkiyi yönetir
     */
    @OneToMany(mappedBy = "brand",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)  // Parent'sız kalan modeller otomatik silinir
    @Builder.Default  // Builder kullanırken bu alan boş liste ile başlar
    private List<VehicleModel> models = new ArrayList<>();

    /**
     * Markaya yeni bir model ekler - İlişki yönetimi metodu
     * Bu metot çift yönlü ilişkiyi (bi-directional) tutarlı tutar
     *
     * @param model Eklenecek araç modeli
     */
    public void addModel(VehicleModel model) {
        // Modeli bu markanın modellerine ekle
        models.add(model);
        // Modelin marka referansını bu marka olarak set et (çift yönlü ilişki tutarlılığı)
        model.setBrand(this);
    }

    /**
     * Markadan bir modeli kaldırır
     * Cascade ve orphanRemoval ayarları sayesinde veritabanından da silinir
     *
     * @param model Kaldırılacak araç modeli
     */
    public void removeModel(VehicleModel model) {
        // Modeli listeden kaldır
        models.remove(model);
        // Modelin marka referansını temizle
        model.setBrand(null);
    }
}
