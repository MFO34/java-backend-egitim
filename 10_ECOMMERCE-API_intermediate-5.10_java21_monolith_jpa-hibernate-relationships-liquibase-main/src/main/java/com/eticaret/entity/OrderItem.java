package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ORDER ITEM ENTITY — Sipariş kalemi
 * Bir siparişin içindeki her bir ürün satırı.
 * Sipariş anındaki fiyat burada saklanır (ürün fiyatı sonra değişse de).
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    /**
     * @ManyToOne — SİPARİŞ
     * FK: order_items.order_id → orders.id
     * nullable = false: Her kalem bir siparişe ait olmak zorunda.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * @ManyToOne — ÜRÜN
     * FK: order_items.product_id → products.id
     * cascade yok: Kalem silinince ürün silinmemeli.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Sipariş ANINDAKİ birim fiyat — Product.price değişse de bu değer sabit kalır
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    // quantity * unitPrice = subtotal
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Ara toplam hesapla
    @PrePersist
    @PreUpdate
    private void calculateSubtotal() {
        if (quantity != null && unitPrice != null) {
            subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
