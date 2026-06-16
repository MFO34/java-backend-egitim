package com.autohub.entity;

// JPA ve Lombok importları
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * İşlem Entity Sınıfı - Kiralama ve Satış İşlemlerini Temsil Eder
 *
 * Bu sınıf hem kiralama hem satış işlemlerini yönetir.
 * İşlem tipine göre farklı alanlar kullanılır.
 *
 * OOP Prensibi - POLİMORFİZM (Polymorphism):
 * TransactionType enum'u ile kiralama ve satış işlemleri farklı davranış gösterir.
 * calculateTotalAmount() metodu işlem tipine göre farklı hesaplamalar yapar.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * İşlem durumu ve toplam tutar hesaplaması kapsüllenmiştir.
 */
@Entity
@Table(name = "transactions",
        indexes = {
            // İşlem numarasına göre benzersiz index
            @Index(name = "idx_transaction_number", columnList = "transaction_number", unique = true),
            // Müşteriye göre işlem sorgulama için
            @Index(name = "idx_transaction_customer", columnList = "customer_id"),
            // Araca göre işlem sorgulama için
            @Index(name = "idx_transaction_vehicle", columnList = "vehicle_id"),
            // Tarih bazlı raporlama için
            @Index(name = "idx_transaction_date", columnList = "transaction_date"),
            // İşlem tipi ve durumu için bileşik index
            @Index(name = "idx_transaction_type_status", columnList = "transaction_type, status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"vehicle", "customer"})
public class Transaction extends BaseEntity {

    /**
     * Benzersiz işlem numarası - kullanıcıya görüntülenen referans numarası
     * Otomatik üretilir: TRX-20240115-0001 formatında
     */
    @Column(name = "transaction_number", nullable = false, unique = true, length = 30)
    private String transactionNumber;

    /**
     * İşlem tipi - kiralama mı satış mı?
     */
    @NotNull(message = "İşlem tipi zorunludur")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    /**
     * İşlem durumu
     */
    @NotNull(message = "İşlem durumu zorunludur")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * İşleme dahil olan araç
     * Many-to-One: Bir araç birden fazla işlemde olabilir (farklı zamanlarda)
     */
    @NotNull(message = "Araç bilgisi zorunludur")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_transaction_vehicle"))
    private Vehicle vehicle;

    /**
     * İşlemi yapan müşteri
     * Many-to-One: Bir müşteri birden fazla işlem yapabilir
     */
    @NotNull(message = "Müşteri bilgisi zorunludur")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_transaction_customer"))
    private Customer customer;

    /**
     * İşlemi başlatan kullanıcı (çalışan)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id",
                foreignKey = @ForeignKey(name = "fk_transaction_user"))
    private User processedBy;

    /**
     * İşlem tarihi
     */
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /**
     * Kiralama başlangıç tarihi (sadece kiralama işlemleri için)
     */
    @Column(name = "rental_start_date")
    private LocalDate rentalStartDate;

    /**
     * Kiralama bitiş tarihi (sadece kiralama işlemleri için)
     * Bu tarih tahmini iade tarihidir
     */
    @Column(name = "rental_end_date")
    private LocalDate rentalEndDate;

    /**
     * Gerçek iade tarihi - kiralama bitişinde doldurulur
     */
    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    /**
     * Araç teslim alındığındaki kilometre değeri
     */
    @Column(name = "pickup_mileage")
    private Integer pickupMileage;

    /**
     * Araç iade edildiğindeki kilometre değeri
     */
    @Column(name = "return_mileage")
    private Integer returnMileage;

    /**
     * Temel işlem tutarı (kiralama günlük ücreti * gün sayısı veya satış fiyatı)
     */
    @NotNull(message = "Tutar zorunludur")
    @DecimalMin(value = "0.0", message = "Tutar negatif olamaz")
    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    /**
     * İndirim tutarı
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Vergi tutarı (KDV %18)
     */
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    /**
     * Ek ücretler (hasar, gecikme ücreti vs.)
     */
    @Column(name = "additional_charges", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal additionalCharges = BigDecimal.ZERO;

    /**
     * Toplam ödeme tutarı = baseAmount - discountAmount + taxAmount + additionalCharges
     */
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Ödeme yöntemi
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    /**
     * İşlem notları - özel durumlar için
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Toplam tutarı hesaplar - iş mantığı entity içinde kapsüllenmiş
     * OOP Prensibi: Domain logic entity'de olur (Domain Model pattern)
     *
     * @return Hesaplanmış toplam tutar
     */
    public BigDecimal calculateTotalAmount() {
        // Temel tutar kontrolü - null ise sıfır kabul et
        BigDecimal base = baseAmount != null ? baseAmount : BigDecimal.ZERO;
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal additional = additionalCharges != null ? additionalCharges : BigDecimal.ZERO;

        // KDV hesaplama: %18 vergi oranı
        BigDecimal taxRate = new BigDecimal("0.18");
        BigDecimal netAmount = base.subtract(discount);
        this.taxAmount = netAmount.multiply(taxRate);

        // Toplam: (temel - indirim) + KDV + ek ücretler
        this.totalAmount = netAmount.add(taxAmount).add(additional);
        return this.totalAmount;
    }

    /**
     * Kiralama gün sayısını hesaplar
     *
     * @return Kiralama süresi (gün olarak), kiralama değilse 0
     */
    public long getRentalDays() {
        // Kiralama işlemi değilse 0 döndür
        if (!TransactionType.RENTAL.equals(transactionType)) return 0;
        // Başlangıç veya bitiş tarihi yoksa 0 döndür
        if (rentalStartDate == null || rentalEndDate == null) return 0;
        // Başlangıç ile bitiş arasındaki gün farkını hesapla
        return ChronoUnit.DAYS.between(rentalStartDate, rentalEndDate);
    }

    /**
     * İşlem tipi enum - kiralamak mı satmak mı?
     */
    public enum TransactionType {
        RENTAL,  // Kiralama işlemi
        SALE     // Satış işlemi
    }

    /**
     * İşlem durumu enum
     */
    public enum TransactionStatus {
        PENDING,    // Beklemede - henüz onaylanmadı
        CONFIRMED,  // Onaylandı - işlem gerçekleşti
        ACTIVE,     // Aktif - kiralama devam ediyor
        COMPLETED,  // Tamamlandı - araç iade edildi veya satış tamam
        CANCELLED,  // İptal edildi
        OVERDUE     // Gecikmiş - araç zamanında iade edilmedi
    }

    /**
     * Ödeme yöntemi enum
     */
    public enum PaymentMethod {
        CASH,           // Nakit
        CREDIT_CARD,    // Kredi kartı
        DEBIT_CARD,     // Banka kartı
        BANK_TRANSFER,  // Havale/EFT
        INSTALLMENT     // Taksit
    }
}
