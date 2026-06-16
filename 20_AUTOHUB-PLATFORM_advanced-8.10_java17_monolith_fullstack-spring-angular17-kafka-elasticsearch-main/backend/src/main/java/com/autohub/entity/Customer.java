package com.autohub.entity;

// JPA, Lombok ve doğrulama importları
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Müşteri Entity Sınıfı
 *
 * Araç kiralayan veya satın alan kişilerin bilgilerini tutar.
 *
 * OOP Prensibi - KALITIM (Inheritance):
 * BaseEntity'den miras alarak temel alanları elde eder.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Müşteri bilgileri private alanlarla kapsüllenmiş, erişim getter/setter ile sağlanır.
 */
@Entity
@Table(name = "customers",
        indexes = {
            // TC Kimlik Numarasına göre benzersiz index
            @Index(name = "idx_customer_tc", columnList = "tc_identity_number", unique = true),
            // E-posta adresine göre benzersiz index
            @Index(name = "idx_customer_email", columnList = "email", unique = true),
            // Telefon numarasına göre arama için index
            @Index(name = "idx_customer_phone", columnList = "phone_number"),
            // Müşteri tipi bazlı sorgular için
            @Index(name = "idx_customer_type", columnList = "customer_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "transactions")  // Sonsuz döngü önleme
public class Customer extends BaseEntity {

    /**
     * T.C. Kimlik Numarası - Türkiye'de resmi kimlik numarası
     * 11 haneli, benzersiz olmalı
     */
    @NotBlank(message = "TC Kimlik Numarası zorunludur")
    @Pattern(regexp = "^[1-9][0-9]{10}$",
             message = "Geçersiz TC Kimlik Numarası formatı")
    @Column(name = "tc_identity_number", nullable = false, unique = true, length = 11)
    private String tcIdentityNumber;

    /**
     * Müşterinin adı
     */
    @NotBlank(message = "Ad zorunludur")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    /**
     * Müşterinin soyadı
     */
    @NotBlank(message = "Soyad zorunludur")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    /**
     * E-posta adresi - iletişim ve bildirimler için kullanılır
     */
    @NotBlank(message = "E-posta adresi zorunludur")
    @Email(message = "Geçersiz e-posta formatı")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Telefon numarası - Türkiye formatı
     * Örnek: +90 555 123 4567
     */
    @Pattern(regexp = "^(\\+90|0)?[5][0-9]{9}$",
             message = "Geçersiz telefon numarası formatı")
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    /**
     * Doğum tarihi - yaş hesaplama ve limit kontrolleri için
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * Ehliyet numarası - araç kiralama işlemleri için zorunlu
     */
    @Column(name = "driving_license_number", length = 20)
    private String drivingLicenseNumber;

    /**
     * Ehliyet geçerlilik tarihi - süresi dolmuş ehliyet ile kiralama yapılamaz
     */
    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    /**
     * Müşteri adresi
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * Şehir bilgisi
     */
    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    /**
     * Posta kodu
     */
    @Pattern(regexp = "^[0-9]{5}$", message = "Geçersiz posta kodu formatı")
    @Column(name = "postal_code", length = 5)
    private String postalCode;

    /**
     * Müşteri tipi - bireysel veya kurumsal
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType = CustomerType.INDIVIDUAL;

    /**
     * Kurumsal müşteriler için şirket adı
     */
    @Column(name = "company_name", length = 200)
    private String companyName;

    /**
     * Kurumsal müşteriler için vergi numarası
     */
    @Column(name = "tax_number", length = 11)
    private String taxNumber;

    /**
     * Müşteri notu - özel istekler veya önemli bilgiler
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Müşteriye ait tüm işlemler (kiralamalar ve satışlar)
     * One-to-Many ilişkisi: Bir müşteri birden fazla işlem yapabilir
     */
    @OneToMany(mappedBy = "customer",
               cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    /**
     * Müşterinin tam adını döndürür
     * Bu metot firstName ve lastName alanlarını birleştirir
     *
     * @return Ad ve soyadı birleştirilmiş tam isim
     */
    public String getFullName() {
        // Ad ve soyadı boşlukla birleştir
        return firstName + " " + lastName;
    }

    /**
     * Müşterinin ehliyet geçerliliğini kontrol eder
     * Süresi dolmuş ehliyet ile kiralama yapılamaz
     *
     * @return true ise ehliyet geçerli, kiralama yapılabilir
     */
    public boolean hasValidLicense() {
        // Ehliyet numarası yoksa geçersiz
        if (drivingLicenseNumber == null || drivingLicenseNumber.isEmpty()) {
            return false;
        }
        // Ehliyet bitiş tarihi yoksa veya bugünden sonra ise geçerli
        return licenseExpiryDate != null && licenseExpiryDate.isAfter(LocalDate.now());
    }

    /**
     * Müşteri yaşını hesaplar
     *
     * @return Müşterinin yaşı, doğum tarihi yoksa -1 döner
     */
    public int getAge() {
        // Doğum tarihi belirtilmemişse -1 döndür
        if (birthDate == null) return -1;
        // Bugünkü tarihten doğum tarihini çıkararak yaşı hesapla
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    /**
     * Müşteri tipi enum sınıfı
     */
    public enum CustomerType {
        INDIVIDUAL,   // Bireysel müşteri
        CORPORATE     // Kurumsal müşteri
    }
}
