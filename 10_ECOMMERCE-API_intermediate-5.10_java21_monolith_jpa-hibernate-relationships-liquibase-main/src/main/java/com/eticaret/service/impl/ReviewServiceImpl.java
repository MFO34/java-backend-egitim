package com.eticaret.service.impl;

import com.eticaret.dto.request.AddReviewRequest;
import com.eticaret.dto.response.ReviewResponse;
import com.eticaret.entity.Review;
import com.eticaret.exception.BusinessException;
import com.eticaret.exception.ResourceNotFoundException;
import com.eticaret.mapper.ReviewMapper;
import com.eticaret.repository.ProductRepository;
import com.eticaret.repository.ReviewRepository;
import com.eticaret.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * YORUM (REVIEW) SERVİSİ — İş Mantığı Katmanı
 * ================================================
 *
 * Ürün yorumları için temel iş kuralları:
 *   1. Her kullanıcı bir ürüne yalnızca bir yorum yapabilir (tekrar yorum yasak)
 *   2. Yorum eklenince isApproved=false olur — moderasyon süreci gerektirir
 *   3. Müşteriye yalnızca onaylanmış yorumlar gösterilir
 *   4. Admin onay/silme yetkisine sahiptir
 *
 * Moderasyon akışı:
 *   Kullanıcı yorum yazar → isApproved=false → Admin onaylar → isApproved=true → Müşteri görür
 *   Bu desen spam ve hakaret yorumlarının engellenmesi için kullanılır.
 *
 * Rating hesaplama:
 *   Ortalama rating ProductServiceImpl.buildProductResponse()'da
 *   reviewRepository.findAverageRatingByProductId() ile hesaplanır.
 *   Her yorum onaylanınca otomatik güncellenir — ayrı bir hesaplama adımı yoktur.
 */
@Service
@Slf4j
public class ReviewServiceImpl {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                              UserRepository userRepository,
                              ProductRepository productRepository,
                              ReviewMapper reviewMapper) {
        this.reviewRepository  = reviewRepository;
        this.userRepository    = userRepository;
        this.productRepository = productRepository;
        this.reviewMapper      = reviewMapper;
    }

    /**
     * Yeni yorum ekler.
     *
     * Neden tekrar yorum kontrolü önce yapılır?
     *   Gereksiz kullanıcı/ürün DB sorgularından kaçınmak için.
     *   Eğer zaten yorum varsa hemen dön — user/product sorgusu yapma (early return).
     *
     * isApproved = false neden?
     *   Kötü niyetli içerik, hakaret veya spam yorumların canlıya geçmemesi için.
     *   Admin panelinden onaylanan yorumlar müşteriye gösterilir.
     *   Alternatif tasarım: Otomatik onay (güvenilir kullanıcılar için) — burada seçilmedi.
     *
     * Neden kullanıcı ve ürün ayrı ayrı yükleniyor?
     *   Review entity'si hem User hem Product'a FK tutar.
     *   JPA ilişki bütünlüğü için yönetilen entity referansı gerekir —
     *   sadece ID ile setUser(new User(id)) çalışmaz, proxy nesne lazım.
     */
    @Transactional
    public ReviewResponse addReview(AddReviewRequest request) {
        // Kullanıcı bu ürüne daha önce yorum yaptı mı? (iş kuralı: bir kullanıcı, bir ürün, bir yorum)
        if (reviewRepository.existsByUserIdAndProductId(request.userId(), request.productId())) {
            throw new BusinessException(
                "Bu ürüne zaten bir yorum yaptınız.", "ALREADY_REVIEWED");
        }

        // Kullanıcıyı DB'den yükle — JPA managed entity gerekir (FK için)
        var user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", request.userId()));

        // Silinmiş ürüne yorum yapılamaz — filter(p -> getIsActive()) bunu sağlar
        var product = productRepository.findById(request.productId())
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Ürün", request.productId()));

        var review = Review.builder()
            .user(user)
            .product(product)
            .rating(request.rating())       // 1-5 arası
            .title(request.title())
            .content(request.content())
            .isApproved(false)              // Moderasyon bekliyor — admin onaylayana kadar gizli
            .build();

        var saved = reviewRepository.save(review);
        log.info("Yorum eklendi (moderasyon bekliyor): userId={}, productId={}, rating={}",
            request.userId(), request.productId(), request.rating());
        return reviewMapper.toResponse(saved);
    }

    /**
     * Ürünün onaylanmış yorumlarını sayfalı getirir.
     *
     * @Transactional(readOnly=true): Okuma işlemi — Hibernate flush etmez, performans artar.
     *
     * findByProductIdAndIsApprovedTrue: Yalnızca onaylananlar döner.
     *   isApproved=false olan yorumlar müşteriye gösterilmez.
     *   Admin panelinde ise tüm yorumlar gösterilir (ayrı endpoint ile).
     *
     * Pageable parametresi: Controller'dan gelir (page, size, sort).
     *   Büyük ürünlerde yüzlerce yorum olabilir — hepsini getirmek OutOfMemory riski.
     *   Sayfalama zorunlu.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        // Ürün gerçekten var mı kontrol et (silinmiş ürünün yorumları isteniyor olabilir)
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Ürün", productId);
        }
        // Yalnızca onaylanmış yorumları döndür — müşteri güvenliği
        return reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable)
            .map(reviewMapper::toResponse);
    }

    /**
     * Yorumu onaylar (Admin işlemi).
     *
     * Admin bu metodu çağırdığında isApproved=true olur.
     * Bir sonraki getProductReviews() çağrısında bu yorum artık görünür.
     *
     * @Transactional: findById + save atomik olmalı.
     *   İki ayrı transaction'da yapılırsa dirty check çalışmaz, save unutulabilir.
     */
    @Transactional
    public ReviewResponse approveReview(Long reviewId) {
        var review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Yorum", reviewId));

        // Onaylanmamış → onaylandı (müşteri artık görecek)
        review.setIsApproved(true);

        // JPA dirty checking: isApproved değişti → commit'te UPDATE otomatik atılır
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    /**
     * Yorumu siler (Admin işlemi — hakaret/spam için).
     *
     * Neden soft delete değil hard delete?
     *   Yorumlar kullanıcı içeriğidir. Hakaret/spam durumunda tamamen silinmesi gerekir.
     *   Ürünlerde soft delete (isActive=false) kullanılıyor çünkü sipariş geçmişinde hâlâ referans lazım.
     *   Yorumda böyle bir tarihsel referans ihtiyacı yok.
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        var review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Yorum", reviewId));
        // Hard delete — DB'den tamamen kaldır
        reviewRepository.delete(review);
    }
}
