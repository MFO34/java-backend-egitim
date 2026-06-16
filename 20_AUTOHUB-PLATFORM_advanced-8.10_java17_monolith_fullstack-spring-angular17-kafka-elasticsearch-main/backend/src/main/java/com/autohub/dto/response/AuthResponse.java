package com.autohub.dto.response;

import lombok.*;

import java.util.Set;

/**
 * Kimlik Doğrulama Yanıt DTO
 *
 * Başarılı login işlemi sonrası client'a dönen JWT token bilgilerini içerir.
 * Access token + Refresh token çifti ile güvenli oturum yönetimi sağlanır.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /**
     * JWT Access Token - API isteklerinde Authorization header'da kullanılır
     * Kısa ömürlüdür (24 saat), süresi dolunca refresh token ile yenilenir
     */
    private String accessToken;

    /**
     * JWT Refresh Token - Access token yenilemek için kullanılır
     * Uzun ömürlüdür (7 gün), güvenli bir yerde saklanmalı
     */
    private String refreshToken;

    /**
     * Token tipi - her zaman "Bearer" olur
     * Authorization header: "Bearer <accessToken>"
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token ömrü (saniye cinsinden)
     * Frontend bu değeri kullanarak token yenileme zamanlaması yapabilir
     */
    private Long expiresIn;

    /**
     * Giriş yapan kullanıcı adı
     */
    private String username;

    /**
     * Giriş yapan kullanıcının e-postası
     */
    private String email;

    /**
     * Kullanıcının tam adı
     */
    private String fullName;

    /**
     * Kullanıcının rolleri - frontend yetkilendirme kontrolü için
     * Örnek: ["ADMIN", "MANAGER"]
     */
    private Set<String> roles;
}
