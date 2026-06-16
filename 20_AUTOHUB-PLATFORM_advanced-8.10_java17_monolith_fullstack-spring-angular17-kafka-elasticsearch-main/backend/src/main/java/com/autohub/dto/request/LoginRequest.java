package com.autohub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Kullanıcı Giriş İsteği DTO
 *
 * Spring Security'nin kimlik doğrulama mekanizmasına veri sağlar.
 * Kullanıcı adı ve şifre ile giriş yapılır.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * Kullanıcı adı - sisteme kayıtlı olmalı
     */
    @NotBlank(message = "Kullanıcı adı zorunludur")
    private String username;

    /**
     * Şifre - doğrulanacak
     * toString metoduna dahil edilmez (güvenlik)
     */
    @NotBlank(message = "Şifre zorunludur")
    @ToString.Exclude  // Loglarda şifre görünmesin
    private String password;
}
