package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ABSTRACT BASE ENTITY — Tüm entity'lerin ortak alanları
 * ========================================================
 *
 * @MappedSuperclass:
 *   Bu sınıf ayrı bir tablo OLUŞTURMAZ.
 *   Alt entity'lerin tablolarına bu alanlar eklenir.
 *   → Kod tekrarını önler (DRY prensibi)
 *
 * @EntityListeners(AuditingEntityListener.class):
 *   Spring Data JPA Auditing mekanizmasını aktif eder.
 *   @CreatedDate, @LastModifiedDate, @CreatedBy → otomatik doldurulur.
 *   Bunun için JpaAuditingConfig sınıfında @EnableJpaAuditing gerekir.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * @Id: Bu alan birincil anahtardır.
     * @GeneratedValue(IDENTITY): Veritabanı otomatik arttırır (SERIAL/AUTO_INCREMENT).
     *   SEQUENCE: Veritabanı sequence nesnesi kullanır (Hibernate varsayılanı)
     *   IDENTITY: Sütun kendisi arttırır (PostgreSQL SERIAL)
     *   AUTO: JPA sağlayıcı seçer
     *   TABLE: Ayrı bir tablo tutar (eski, yavaş — kullanma)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @CreatedDate: Entity ilk kaydedildiğinde otomatik doldurulur.
     * updatable = false: Bir kez yazılır, güncellenmez.
     * Hibernate bunu INSERT anında sağlar — setter'a gerek yok.
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * @LastModifiedDate: Her güncelleme (UPDATE) sonrası otomatik değişir.
     * INSERT ve UPDATE anında doldurulur.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @CreatedBy: Kaydı oluşturan kullanıcının kimliği.
     * AuditorAwareImpl sınıfından alınır (Spring Security entegrasyonu).
     * updatable = false: İlk yazan değişmez.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * @Version — OPTİMİSTİK KİLİTLEME (Optimistic Locking)
     * ========================================================
     * Problem: İki kullanıcı aynı ürünün stokunu eş zamanlı düşürürse?
     *   Kullanıcı A: stok=10 → sipariş ver → stok=9
     *   Kullanıcı B: stok=10 → sipariş ver → stok=9 (YANLIŞ! Gerçek: 8 olmalı)
     *
     * @Version çözümü:
     *   Her UPDATE'te version artar: 0 → 1 → 2 → ...
     *   Hibernate: UPDATE products SET stock=?, version=2 WHERE id=1 AND version=1
     *   Eğer version eşleşmezse → OptimisticLockException fırlatır
     *   Service katmanı bunu yakalar ve tekrar dener.
     *
     * Kilitli satır yoktur → performans yüksek (iyimser kilitleme)
     * Çakışma nadirdir varsayımı → "iyimser" adı buradan gelir
     */
    @Version
    @Column(nullable = false)
    private Long version;
}
