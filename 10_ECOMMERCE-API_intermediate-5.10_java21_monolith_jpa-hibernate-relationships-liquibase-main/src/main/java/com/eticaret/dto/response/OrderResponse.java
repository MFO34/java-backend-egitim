package com.eticaret.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    Long userId,
    String userFullName,
    String statusCode,          // OrderStatus.code()
    String statusDisplayName,   // OrderStatus.displayName()
    String paymentStatusCode,
    String paymentStatusDisplayName,
    BigDecimal totalAmount,
    String shippingAddress,
    String notes,
    List<OrderItemResponse> items,
    LocalDateTime createdAt
) {}
