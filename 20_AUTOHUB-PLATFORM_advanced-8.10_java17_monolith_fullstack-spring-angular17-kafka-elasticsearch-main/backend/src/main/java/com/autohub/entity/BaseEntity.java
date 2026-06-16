package com.autohub.entity;

// JPA ve Java kalıcılık katmanı için gerekli importlar
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tüm Entity sınıflarının temel sınıfı - Soyutlama (Abstraction) prensibi
 *
 * OOP Prensibi - SOYUTLAMA (Abstraction):
 * Bu abstract sınıf, tüm varlıkların ortak özelliklerini (id, oluşturma tarihi,
 * güncellenme tarihi, vs.) tek bir yerde toplar. Her entity bu sınıfı extend ederek
 * tekrar eden kodu önler ve DRY (Don't Repeat Yourself) prensibine uyar.
 *
 * OOP Prensibi - KALITIM (Inheritance):
 * Vehicle, Brand, Customer gibi tüm entity'ler bu sınıfı extend eder ve
 * ortak alanları otomatik olarak miras alır.
 *
 * Serializable implementasyonu: Entity'lerin Redis'te serileştirilebilmesi için gerekli
 */
@Getter              // Lombok: Tüm alanlar için getter metodları oluşturur
@Setter              // Lombok: Tüm alanlar için setter metodları oluşturur
@MappedSuperclass    // Bu sınıf doğrudan tablo oluşturmaz, sadece alanları miras verir
@EntityListeners(AuditingEntityListener.class)  // JPA Auditing - tarih alanlarını otomatik doldurur
public abstract class BaseEntity implements Serializable {

    // Serialization versiyonu - sınıf değişikliklerinde uyumsuzlukları önler
    private static final long serialVersionUID = 1L;

    /**
     * Birincil anahtar (Primary Key) - Her entity'nin benzersiz tanımlayıcısı
     * GenerationType.IDENTITY: Veritabanı auto-increment özelliğini kullanır (PostgreSQL: SERIAL)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Kaydın oluşturulma tarihi - JPA Auditing ile otomatik set edilir
     * updatable = false: Bu alan bir kez set edilir ve asla güncellenmez
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Kaydın son güncellenme tarihi - Her kaydet işleminde otomatik güncellenir
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Kaydı oluşturan kullanıcının adı - Güvenlik bağlamından otomatik alınır
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    /**
     * Kaydı son güncelleyen kullanıcının adı - Her güncelleme anında set edilir
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Soft delete (yumuşak silme) özelliği
     * Gerçek silme yerine bu alan true yapılır, böylece veri kaybolmaz
     * Veri geçmişi ve audit trail için kritik öneme sahip
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * Kaydın aktif/pasif durumu
     * Sistemde geçici olarak devre dışı bırakılmak istenen kayıtlar için kullanılır
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Nesneyi soft-delete ile işaretler
     * Veritabanından fiziksel olarak silmek yerine isDeleted flag'ini true yapar
     */
    public void softDelete() {
        // Nesneyi silinmiş olarak işaretle - fiziksel silme yapma
        this.isDeleted = true;
        // Silme işleminde nesne pasif hale gelir
        this.isActive = false;
    }

    /**
     * İki entity'nin eşit olup olmadığını kontrol eder
     * ID bazlı karşılaştırma yapılır - JPA best practice
     */
    @Override
    public boolean equals(Object obj) {
        // Aynı nesne referansı ise kesinlikle eşittir
        if (this == obj) return true;
        // Null veya farklı sınıf ise eşit değildir
        if (obj == null || getClass() != obj.getClass()) return false;
        // ID karşılaştırması yap
        BaseEntity that = (BaseEntity) obj;
        return id != null && id.equals(that.id);
    }

    /**
     * Hash kod üretimi - equals ile tutarlı olmalı
     * ID üzerinden hash code üretilir
     */
    @Override
    public int hashCode() {
        // ID null ise varsayılan hash code kullan, değilse ID'nin hash'ini döndür
        return id != null ? id.hashCode() : super.hashCode();
    }
}
