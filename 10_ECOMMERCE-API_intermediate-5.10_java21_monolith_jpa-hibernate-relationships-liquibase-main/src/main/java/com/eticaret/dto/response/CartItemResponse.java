package com.eticaret.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
    Long id,
    Long productId,
    String productName,
    String productSlug,
    String primaryImageUrl,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal subtotal,
    Boolean inStock
) {}
