package com.auth.dto.response;

import java.time.LocalDateTime;

// Basit mesaj yanıtı — email gönderildi, şifre sıfırlandı vb.
public record MessageResponse(
    String message,
    LocalDateTime timestamp
) {
    public MessageResponse(String message) {
        this(message, LocalDateTime.now());
    }
}
