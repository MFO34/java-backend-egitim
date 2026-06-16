package com.eticaret.specification;

import com.eticaret.entity.Category;
import com.eticaret.entity.Product;
import com.eticaret.entity.Tag;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * SPECIFICATION PATTERN — Dinamik JPA Sorguları
 * ================================================
 * Problem:
 *   Kullanıcı şu filtrelerden herhangi birini uygulayabilir:
 *   - keyword (ad/açıklama arama)
 *   - categoryId
 *   - minPrice / maxPrice
 *   - tagSlug
 *   - inStockOnly
 *
 *   Tüm kombinasyonlar için ayrı ayrı @Query metodu yazmak imkansız!
 *   2^5 = 32 farklı kombinasyon → 32 metod!
 *
 * Çözüm — Specification Pattern:
 *   Her filtre ayrı bir Specification (JPA Predicate üreten fonksiyon).
 *   Specification'lar .and() / .or() ile birleştirilir.
 *   JpaSpecificationExecutor.findAll(spec, pageable) ile çalıştırılır.
 *
 * Nasıl Çalışır?
 *   Specification<Product> → toPredicate(root, query, criteriaBuilder)
 *   root: FROM products p (entity erişimi)
 *   query: SELECT ... (sorgu düzenleme)
 *   criteriaBuilder: WHERE koşulları oluşturma (cb.like, cb.between, cb.join)
 */
public class ProductSpecification {

    /**
     * Anahtar kelimeyle arama — ad veya açıklamada.
     * SQL eşdeğeri: WHERE LOWER(p.name) LIKE '%keyword%'
     *                  OR LOWER(p.description) LIKE '%keyword%'
     */
    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            // keyword boşsa → bu filtre uygulanmaz (null döndür)
            if (!StringUtils.hasText(keyword)) return null;

            var lowerKeyword = "%" + keyword.toLowerCase() + "%";

            // cb.lower: Büyük/küçük harf duyarsız arama
            var namePredicate = cb.like(cb.lower(root.get("name")), lowerKeyword);
            var descPredicate = cb.like(cb.lower(root.get("description")), lowerKeyword);

            // OR koşulu: ad VEYA açıklama içeriyorsa
            return cb.or(namePredicate, descPredicate);
        };
    }

    /**
     * Kategoriye göre filtre.
     * JOIN ile alt kategoriler dahil edilebilir (hiyerarşik).
     */
    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;

            // root.get("category") → ManyToOne ilişki
            // root.get("category").get("id") → category.id alanı
            Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);
            return cb.equal(categoryJoin.get("id"), categoryId);
        };
    }

    /**
     * Fiyat aralığı filtresi.
     * SQL eşdeğeri: WHERE p.price BETWEEN :minPrice AND :maxPrice
     */
    public static Specification<Product> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;

            var predicates = new ArrayList<Predicate>();

            if (min != null) {
                // >= minPrice
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), min));
            }
            if (max != null) {
                // <= maxPrice
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), max));
            }

            // AND ile birleştir: price >= min AND price <= max
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Etikete göre filtre — ManyToMany JOIN.
     * SQL eşdeğeri: INNER JOIN product_tags pt ON p.id = pt.product_id
     *               INNER JOIN tags t ON pt.tag_id = t.id
     *               WHERE t.slug = :tagSlug
     */
    public static Specification<Product> hasTag(String tagSlug) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(tagSlug)) return null;

            // ManyToMany join: Product.tags → Tags koleksiyonu
            Join<Product, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return cb.equal(tagJoin.get("slug"), tagSlug);
        };
    }

    /**
     * Stokta olan ürünler.
     * SQL eşdeğeri: WHERE p.stock_quantity > 0
     */
    public static Specification<Product> isInStock() {
        return (root, query, cb) ->
            cb.greaterThan(root.get("stockQuantity"), 0);
    }

    /**
     * Sadece aktif ürünler.
     * Neredeyse her sorguda kullanılır.
     */
    public static Specification<Product> isActive() {
        return (root, query, cb) ->
            cb.equal(root.get("isActive"), true);
    }

    /**
     * İndirimde olan ürünler (originalPrice > price).
     */
    public static Specification<Product> isOnSale() {
        return (root, query, cb) ->
            cb.and(
                cb.isNotNull(root.get("originalPrice")),
                cb.greaterThan(root.get("originalPrice"), root.get("price"))
            );
    }

    /**
     * SPEC BİRLEŞTİRME — Kullanım örneği:
     * ========================================
     * Service katmanında bu statik metodlar birleştirilir:
     *
     *   Specification<Product> spec = Specification
     *       .where(ProductSpecification.isActive())
     *       .and(ProductSpecification.hasKeyword(filter.keyword()))
     *       .and(ProductSpecification.hasCategory(filter.categoryId()))
     *       .and(ProductSpecification.hasPriceBetween(filter.minPrice(), filter.maxPrice()))
     *       .and(ProductSpecification.hasTag(filter.tagSlug()))
     *       .and(filter.inStockOnly() ? ProductSpecification.isInStock() : null);
     *
     *   Page<Product> products = productRepository.findAll(spec, pageable);
     *
     * null Specification'lar otomatik ignore edilir → temiz kod!
     */

    /**
     * Tüm filtreleri birleştiren yardımcı metod.
     * Request DTO'dan Specification oluşturur.
     */
    public static Specification<Product> buildFilter(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String tagSlug,
            boolean inStockOnly,
            boolean onSaleOnly) {

        // Specification.where(null) → tüm kayıtlar (başlangıç noktası)
        var spec = Specification.where(isActive());

        if (StringUtils.hasText(keyword)) {
            spec = spec.and(hasKeyword(keyword));
        }
        if (categoryId != null) {
            spec = spec.and(hasCategory(categoryId));
        }
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(hasPriceBetween(minPrice, maxPrice));
        }
        if (StringUtils.hasText(tagSlug)) {
            spec = spec.and(hasTag(tagSlug));
        }
        if (inStockOnly) {
            spec = spec.and(isInStock());
        }
        if (onSaleOnly) {
            spec = spec.and(isOnSale());
        }

        return spec;
    }
}
