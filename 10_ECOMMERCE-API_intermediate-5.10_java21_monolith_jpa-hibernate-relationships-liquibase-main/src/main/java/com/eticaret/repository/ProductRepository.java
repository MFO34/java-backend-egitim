package com.eticaret.repository;

import com.eticaret.entity.Product;
import com.eticaret.repository.projection.ProductProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * PRODUCT REPOSITORY — En kapsamlı repository
 * =============================================
 * JpaRepository: Temel CRUD işlemleri
 * JpaSpecificationExecutor: Dinamik sorgular (Specification pattern)
 *   → findAll(Specification<Product> spec, Pageable pageable) metodu gelir
 */
@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    // ===== METHOD NAMING SORGULARI =====

    // Aktif ürünleri sayfalı getir
    Page<Product> findByIsActiveTrue(Pageable pageable);

    // Kategoriye göre aktif ürünler
    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    // Stokta olan ürünler
    List<Product> findByStockQuantityGreaterThanAndIsActiveTrue(int minStock);

    // Fiyat aralığı
    Page<Product> findByPriceBetweenAndIsActiveTrue(
        BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // ===== JPQL SORGUSU — TEXT BLOCK (Java 15+) =====
    /**
     * Text Block (Java 15+):
     *   """ ... """ içinde çok satırlı string yazılır.
     *   Girinti otomatik kaldırılır.
     *   SQL/JPQL sorgularında okunabilirliği artırır.
     *
     * DISTINCT: JOIN sonrası ürün çoklanmasını önler.
     * JOIN FETCH: N+1 problemini çözer — tek sorguda yüklenir.
     */
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category c
        LEFT JOIN FETCH p.images i
        WHERE p.isActive = true
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    /**
     * N+1 SORUNU VE ÇÖZÜMÜ — @EntityGraph
     * ======================================
     * Sorun:
     *   Ürün listesi çekildiğinde her ürün için:
     *   - category → ayrı SELECT
     *   - images   → ayrı SELECT
     *   100 ürün → 1 + 100 + 100 = 201 sorgu!
     *
     * @EntityGraph çözümü:
     *   attributePaths = {"category", "images"}
     *   → Tek JOIN sorgusu ile hepsi gelir:
     *   SELECT p.*, c.*, i.* FROM products p
     *   LEFT JOIN categories c ON p.category_id = c.id
     *   LEFT JOIN product_images i ON i.product_id = p.id
     *   WHERE p.is_active = true
     *
     * Ne zaman kullanılır?
     *   Listede tüm ürünlerin kategori ve görseli gösteriliyorsa.
     *   Detay sayfasında ise normal findById yeterli.
     */
    @EntityGraph(attributePaths = {"category", "images"})
    Page<Product> findAllByIsActiveTrue(Pageable pageable);

    /**
     * JOIN FETCH ile N+1 Çözümü (alternatif):
     * @EntityGraph yerine JPQL JOIN FETCH kullanılabilir.
     * Fark: @EntityGraph daha deklaratif, JOIN FETCH daha esnek.
     */
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.tags
        WHERE p.isActive = true AND p.category.id = :categoryId
        """)
    List<Product> findByCategoryWithTagsJoinFetch(@Param("categoryId") Long categoryId);

    /**
     * NATIVE SQL SORGUSU
     * ====================
     * PostgreSQL'e özgü özellikler kullanılabilir.
     * countQuery: Sayfalama için ayrı COUNT sorgusu (performans için).
     */
    @Query(value = """
        SELECT p.* FROM products p
        INNER JOIN product_tags pt ON p.id = pt.product_id
        INNER JOIN tags t ON pt.tag_id = t.id
        WHERE t.slug = :tagSlug AND p.is_active = true
        ORDER BY p.created_at DESC
        """,
        countQuery = "SELECT COUNT(DISTINCT p.id) FROM products p " +
                     "INNER JOIN product_tags pt ON p.id = pt.product_id " +
                     "INNER JOIN tags t ON pt.tag_id = t.id " +
                     "WHERE t.slug = :tagSlug AND p.is_active = true",
        nativeQuery = true)
    Page<Product> findByTagSlug(@Param("tagSlug") String tagSlug, Pageable pageable);

    /**
     * @Modifying — Stok azalt (UPDATE sorgusu)
     * ==========================================
     * @Query ile normalde SELECT yapılır.
     * @Modifying: UPDATE/DELETE sorgularında gereklidir.
     * clearAutomatically = true: Hibernate 1. seviye cache'ini temizle
     *   (stale data olmasın — yeni stok değerini okuyabilelim)
     *
     * Optimistic Locking ile kullanım:
     *   WHERE id = :id AND version = :version
     *   Hibernate bunu otomatik ekler — @Version sayesinde.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.stockQuantity = p.stockQuantity - :quantity
        WHERE p.id = :id AND p.stockQuantity >= :quantity
        """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.stockQuantity = p.stockQuantity + :quantity
        WHERE p.id = :id
        """)
    void increaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * PROJECTION — Sadece ihtiyaç duyulan alanlar
     * ==============================================
     * Sorun: findAll() tüm alanları getirir → gereksiz data transfer
     * Çözüm: Interface Projection — sadece istenilen alanlar
     * ProductProjection → id, name, price, stockQuantity
     *
     * Üretilen SQL:
     *   SELECT id, name, price, stock_quantity FROM products WHERE ...
     *   (description, images vb. GELMEZ → hızlı!)
     */
    @Query("""
        SELECT p.id as id, p.name as name, p.price as price,
               p.stockQuantity as stockQuantity, p.slug as slug
        FROM Product p WHERE p.isActive = true AND p.stockQuantity < :threshold
        """)
    List<ProductProjection> findLowStockProducts(@Param("threshold") int threshold);

    // En çok stok azalmış ürünler (siparişlere göre)
    @Query(value = """
        SELECT p.*, COUNT(oi.id) as order_count
        FROM products p
        JOIN order_items oi ON oi.product_id = p.id
        GROUP BY p.id
        ORDER BY order_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Product> findBestSellingProducts(@Param("limit") int limit);
}
