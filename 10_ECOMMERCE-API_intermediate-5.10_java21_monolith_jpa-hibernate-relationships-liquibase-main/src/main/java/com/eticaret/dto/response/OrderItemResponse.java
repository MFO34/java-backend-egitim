package com.eticaret.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    String productSlug,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {}
