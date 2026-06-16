package com.eticaret.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

// Tüm alanlar opsiyonel — sadece dolu olanlar güncellenir (PATCH davranışı)
public record UpdateProductRequest(
    @Size(max = 300) String name,
    String description,
    @DecimalMin("0.01") BigDecimal price,
    BigDecimal originalPrice,
    @Min(0) Integer stockQuantity,
    Long categoryId,
    List<String> tagNames
) {}
