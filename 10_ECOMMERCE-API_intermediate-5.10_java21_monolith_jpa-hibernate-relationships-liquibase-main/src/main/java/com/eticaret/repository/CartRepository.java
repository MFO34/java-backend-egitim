package com.eticaret.repository;

import com.eticaret.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Kullanıcıya ait sepeti bul
    Optional<Cart> findByUserId(Long userId);

    /**
     * Sepetin kalemlerini JOIN ile yükle.
     * Sepet görüntülendiğinde her kalem + ürün bilgisi gerekli.
     * items.product: CartItem → Product zinciri
     */
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.images"})
    Optional<Cart> findWithItemsByUserId(Long userId);
}
