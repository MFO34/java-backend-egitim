package com.eticaret.dto.response;

import java.math.BigDecimal;

// Ürün listesi için hafif DTO
public record ProductSummaryResponse(
    Long id,
    String name,
    String slug,
    BigDecimal price,
    BigDecimal originalPrice,
    Boolean inStock,
    String categoryName,
    String primaryImageUrl,
    Double averageRating,
    Long reviewCount
) {}
