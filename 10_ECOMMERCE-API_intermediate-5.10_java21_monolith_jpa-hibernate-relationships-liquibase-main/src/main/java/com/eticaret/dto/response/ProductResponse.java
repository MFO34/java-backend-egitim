package com.eticaret.dto.response;

import com.eticaret.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Ürün detay sayfası için tam DTO
public record ProductResponse(
    Long id,
    String name,
    String slug,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stockQuantity,
    Boolean isActive,
    Product.ProductStatus status,
    CategoryResponse category,
    List<ProductImageResponse> images,
    List<TagResponse> tags,
    Double averageRating,
    Long reviewCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
