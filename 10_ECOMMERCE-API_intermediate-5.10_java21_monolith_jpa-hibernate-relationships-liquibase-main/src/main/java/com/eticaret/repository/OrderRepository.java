package com.eticaret.repository;

import com.eticaret.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Kullanıcının siparişleri (tarih azalan)
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    // Belirli status'taki siparişler (String olarak — converter devrede değil)
    // JPQL'de OrderStatus sealed interface field'ını string kodu ile karşılaştır
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    List<Order> findAllByUserId(@Param("userId") Long userId);

    /**
     * @EntityGraph — Sipariş + Kalemler + Ürünler tek sorguda
     * Sipariş detay sayfasında tüm bu bilgiler gerekli.
     * olmadan: order → N adet orderItem → N adet product = 2N+1 sorgu!
     */
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    Optional<Order> findWithItemsById(Long id);

    // Bekleyen ödemelerin sayısı
    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = 'PENDING'")
    long countPendingPayments();
}
