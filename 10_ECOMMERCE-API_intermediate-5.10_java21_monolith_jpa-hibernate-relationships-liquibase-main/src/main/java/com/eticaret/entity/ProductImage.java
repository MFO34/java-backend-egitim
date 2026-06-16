package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * PRODUCT IMAGE ENTITY — @ManyToOne Product ile
 * Bir ürünün birden fazla görseli olabilir.
 * orphanRemoval = true (Product'ta) → product.getImages().remove() → silinir
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage extends BaseEntity {

    // Görsel URL'si (CDN veya storage path)
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    // Ana görsel mi? Ürün listesinde bu görsel gösterilir.
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    // Görsellerin sırası (ana görsel genellikle 0)
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // Erişilebilirlik için alternatif metin
    @Column(name = "alt_text", length = 255)
    private String altText;

    /**
     * @ManyToOne — ÜRÜNe bağlı
     * FK: product_images.product_id → products.id
     * FetchType.LAZY: Görsel yüklenince ürün bilgisi gelmez.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
