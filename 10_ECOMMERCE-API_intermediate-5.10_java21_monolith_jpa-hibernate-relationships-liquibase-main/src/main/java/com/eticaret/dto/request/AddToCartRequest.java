package com.eticaret.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(
    @NotNull(message = "Ürün ID boş olamaz") Long productId,
    @NotNull @Min(value = 1, message = "Miktar en az 1 olmalıdır") Integer quantity
) {}
