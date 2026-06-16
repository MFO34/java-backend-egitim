package com.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TÜM ENTITY'LERİN ORTAK TABANISI
 * @MappedSuperclass: Ayrı tablo oluşturmaz, alanları alt entity'lere ekler.
 * @EntityListeners: Spring Auditing (@CreatedDate, @LastModifiedDate) aktive eder.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    // Birincil anahtar — PostgreSQL SERIAL ile otomatik artar
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kayıt oluşturulma zamanı — Spring tarafından otomatik doldurulur
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // Son güncelleme zamanı — her UPDATE'te otomatik değişir
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
