package com.eticaret.repository;

import com.eticaret.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Ana kategoriler (parent_id = null)
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    // Belirli bir üst kategorinin alt kategorileri
    List<Category> findByParentIdAndIsActiveTrue(Long parentId);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * N+1 Problemi Çözümü — @EntityGraph ile JOIN FETCH
     * ====================================================
     * Problem:
     *   findAll() → SELECT * FROM categories → N adet kategori
     *   Her kategori için: SELECT * FROM categories WHERE parent_id = ? → N sorgu!
     *   Toplam: 1 + N sorgu = N+1 Problemi
     *
     * Çözüm 1 — @EntityGraph:
     *   attributePaths = {"children"}: children koleksiyonunu JOIN ile yükle
     *   → Tek sorguda tüm kategoriler + alt kategoriler
     *
     * Üretilen SQL:
     *   SELECT c.*, ch.* FROM categories c
     *   LEFT JOIN categories ch ON ch.parent_id = c.id
     *   WHERE c.is_active = true
     */
    @Query("""
        SELECT DISTINCT c FROM Category c
        LEFT JOIN FETCH c.children
        WHERE c.parent IS NULL AND c.isActive = true
        ORDER BY c.displayOrder
        """)
    List<Category> findRootCategoriesWithChildren();
}
