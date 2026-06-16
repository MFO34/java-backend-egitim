package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CART ENTITY — Alışveriş sepeti
 * ================================
 * @OneToOne User ile: Her kullanıcının bir sepeti var.
 * @OneToMany CartItem ile: Sepetin içinde kalemler var.
 *
 * FK: carts.user_id → users.id (UNIQUE → her kullanıcı için bir sepet)
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    /**
     * @OneToOne — KULLANICI
     * FK: carts.user_id → users.id
     * unique = true (DB'de): Her kullanıcı için tek sepet garantisi.
     *
     * User tarafında: @OneToOne(mappedBy = "user") → sahip taraf Cart.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * @OneToMany — SEPET KALEMLERİ
     * cascade = ALL + orphanRemoval:
     *   cart.getItems().clear() → tüm kalemler silinir (sepet boşaltılır).
     *   Bu sepetin boşaltma işlemi için kullanışlıdır.
     */
    @OneToMany(mappedBy = "cart",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // Sepet toplam tutarını hesapla
    public BigDecimal getTotalAmount() {
        return items.stream()
            .map(CartItem::getSubtotal)   // Her kalemin ara toplamı
            .reduce(BigDecimal.ZERO, BigDecimal::add);  // Topla
    }

    // Sepetteki toplam ürün adedi
    public int getTotalItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    // Belirli bir ürün sepette var mı?
    public Optional<CartItem> findItem(Long productId) {
        return items.stream()
            .filter(item -> item.getProduct().getId().equals(productId))
            .findFirst();
    }

    // Sepeti boşalt
    public void clear() {
        items.clear();  // orphanRemoval=true → DB'den de silinir
    }
}
