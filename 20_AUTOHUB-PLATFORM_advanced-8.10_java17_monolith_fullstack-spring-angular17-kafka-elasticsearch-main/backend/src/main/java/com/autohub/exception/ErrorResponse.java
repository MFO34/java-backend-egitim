package com.autohub.exception;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Standart Hata Yanıt DTO
 * Tüm hata durumlarında client'a dönen JSON yapısı
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    /** Hatanın oluştuğu zaman */
    private LocalDateTime timestamp;
    /** HTTP durum kodu (404, 409, 500 vs.) */
    private int status;
    /** Kısa hata açıklaması */
    private String error;
    /** Detaylı hata mesajı */
    private String message;
    /** Hatanın oluştuğu endpoint */
    private String path;
}
