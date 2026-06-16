package com.eticaret.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
    @NotBlank(message = "Sipariş durumu boş olamaz")
    String statusCode   // "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"
) {}
