package com.eticaret.repository;

import com.eticaret.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Ürünün onaylı yorumları (sayfalı)
    Page<Review> findByProductIdAndIsApprovedTrue(Long productId, Pageable pageable);

    // Kullanıcının bu ürüne yorumu var mı?
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // Kullanıcının bu ürüne yaptığı yorum
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    // Ürünün ortalama puanı
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    // Ürünün yorum sayısı
    long countByProductIdAndIsApprovedTrue(Long productId);
}
