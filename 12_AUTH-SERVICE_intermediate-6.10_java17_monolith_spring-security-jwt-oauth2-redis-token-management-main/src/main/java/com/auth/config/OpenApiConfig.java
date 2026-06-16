package com.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * SWAGGER / OPENAPI YAPILANDIRMASI
 * =================================
 * Swagger UI'da JWT authentication desteği ekler.
 *
 * @SecurityScheme: "bearerAuth" adında bir güvenlik şeması tanımlar.
 *   - type: HTTP (Bearer token, Basic auth değil)
 *   - scheme: bearer (Authorization: Bearer <token>)
 *   - bearerFormat: JWT (sadece dokümantasyon için)
 *
 * Controller'larda @SecurityRequirement(name = "bearerAuth") ile
 * hangi endpoint'lerin bu token'ı gerektirdiğini belirtiriz.
 *
 * Swagger UI kullanımı:
 *   1. /api/v1/auth/login ile giriş yap → accessToken al
 *   2. Swagger UI'da sağ üst "Authorize" butonuna tıkla
 *   3. "Bearer <accessToken>" gir
 *   4. Korumalı endpoint'leri test et
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Auth Service API",
        version = "1.0.0",
        description = "Spring Security + JWT + OAuth2 + Redis Authentication Service"
    )
)
@SecurityScheme(
    name = "bearerAuth",               // Controller'larda referans verilen isim
    type = SecuritySchemeType.HTTP,    // HTTP authentication
    scheme = "bearer",                 // Bearer token
    bearerFormat = "JWT"               // Token formatı (dokümantasyon)
)
public class OpenApiConfig {
    // Bean'e gerek yok — annotation'lar yeterli
}
