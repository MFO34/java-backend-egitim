package com.eticaret.dto.request;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * PRODUCT FILTER REQUEST — Specification ile dinamik filtreleme
 * Tüm alanlar opsiyonel — belirtilmeyenler filtre oluşturmaz.
 */
public record ProductFilterRequest(
    String keyword,       // Ad / açıklama arama
    Long categoryId,      // Kategori filtresi
    @DecimalMin("0") BigDecimal minPrice,  // Minimum fiyat
    @DecimalMin("0") BigDecimal maxPrice,  // Maksimum fiyat
    String tagSlug,       // Etiket filtresi
    boolean inStockOnly,  // Sadece stokta olanlar
    boolean onSaleOnly    // Sadece indirimde olanlar
) {
    // Compact constructor: varsayılan değerler
    public ProductFilterRequest {
        if (keyword != null) keyword = keyword.trim();
    }
}
