package com.auth.entity;

/**
 * KİMLİK DOĞRULAMA SAĞLAYICISI
 * ================================
 * LOCAL → E-posta + şifre ile kayıt (geleneksel)
 * GOOGLE → Google OAuth2 ile giriş
 * GITHUB → GitHub OAuth2 ile giriş
 *
 * Kullanım: User entity'sinde @Enumerated(STRING) ile saklanır.
 * Bir kullanıcı tek bir provider'a ait — farklı provider'ları
 * birleştirmek için account linking mantığı gerekir (bu scope dışı).
 */
public enum AuthProvider {
    LOCAL,   // Geleneksel e-posta/şifre
    GOOGLE,  // Google OAuth2
    GITHUB   // GitHub OAuth2
}
