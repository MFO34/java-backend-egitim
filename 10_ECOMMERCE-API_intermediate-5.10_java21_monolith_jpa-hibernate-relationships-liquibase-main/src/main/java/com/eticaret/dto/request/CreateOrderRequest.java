package com.eticaret.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Sepetten sipariş oluşturma.
 * Sepetteki tüm kalemler sipariş haline gelir.
 */
public record CreateOrderRequest(
    @NotNull(message = "Kullanıcı ID boş olamaz") Long userId,
    String shippingAddress,  // null → kullanıcının kayıtlı adresi kullanılır
    String notes
) {}
