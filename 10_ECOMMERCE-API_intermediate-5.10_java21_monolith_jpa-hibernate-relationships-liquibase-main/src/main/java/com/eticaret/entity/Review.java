package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * REVIEW ENTITY — Ürün yorumu
 * Bir kullanıcı bir ürüne sadece bir yorum yapabilir (unique constraint).
 */
@Entity
@Table(name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_reviews_user_product",
                          columnNames = {"user_id", "product_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 1-5 arası puan (DB'de CHECK constraint ile)
    @Column(nullable = false)
    private Integer rating;

    @Column(length = 200)
    private String title;

    /**
     * @Lob: Uzun yorum metni için TEXT.
     * Yorumlar çok uzun olabilir → VARCHAR(255) yetmez.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    // Moderasyon: Admin onayı gerekebilir
    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = false;
}
