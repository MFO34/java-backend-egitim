package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * CART ITEM ENTITY — Sepet kalemi
 * Sepetteki her ürün satırı.
 * DB'de unique constraint: (cart_id, product_id) — aynı ürün iki kez eklenemez.
 */
@Entity
@Table(name = "cart_items",
    uniqueConstraints = {
        // Aynı ürün bir sepette bir kez bulunur — miktar güncellenir.
        @UniqueConstraint(name = "uq_cart_items_cart_product",
                          columnNames = {"cart_id", "product_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // Güncel ürün fiyatı üzerinden hesaplanır (@Transient değil — DB'de sakla)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Fiyat değişirse güncelle
    @PrePersist
    @PreUpdate
    private void calculateSubtotal() {
        if (product != null && quantity != null) {
            subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }
}
