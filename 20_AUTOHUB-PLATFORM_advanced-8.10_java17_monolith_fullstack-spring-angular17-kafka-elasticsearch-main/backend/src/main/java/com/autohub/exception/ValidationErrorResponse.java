package com.autohub.exception;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Doğrulama Hatası Yanıt DTO
 * Alan bazında doğrulama hataları için genişletilmiş yanıt yapısı
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    /** Alan adı -> hata mesajı eşleşmeleri */
    private Map<String, String> fieldErrors;
}
