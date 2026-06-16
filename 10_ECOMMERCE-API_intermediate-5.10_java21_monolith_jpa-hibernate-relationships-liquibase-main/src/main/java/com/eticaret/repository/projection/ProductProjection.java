package com.eticaret.repository.projection;

import java.math.BigDecimal;

/**
 * PROJECTION INTERFACE — Kısmi veri transferi
 * =============================================
 * Spring Data JPA, bu interface'i dinamik olarak implement eder.
 * Sadece buradaki getter'lara karşılık gelen alanlar SELECT edilir.
 *
 * Avantajları:
 *   - Az veri → hızlı sorgu
 *   - Gereksiz JOIN yapılmaz
 *   - DTO mapping gerekmez
 *
 * Kullanım: ProductRepository.findLowStockProducts() döner.
 * Repository'de: "p.id as id" → getId() metoduna map edilir.
 */
public interface ProductProjection {
    Long getId();          // p.id
    String getName();      // p.name
    BigDecimal getPrice(); // p.price
    Integer getStockQuantity(); // p.stockQuantity
    String getSlug();      // p.slug
}
