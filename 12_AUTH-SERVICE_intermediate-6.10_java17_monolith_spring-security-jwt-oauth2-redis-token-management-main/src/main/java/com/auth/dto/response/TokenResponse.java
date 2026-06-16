package com.auth.dto.response;

/**
 * TOKEN YANITI — Login ve refresh sonrası döner
 * ===============================================
 * accessToken: Kısa ömürlü (15 dk) — her API isteğinde gönderilir
 * refreshToken: Uzun ömürlü (7 gün) — sadece yeni token almak için
 * tokenType: "Bearer" — Authorization başlığında kullanım şekli
 */
public record TokenResponse(
    String accessToken,       // JWT access token
    String refreshToken,      // Refresh token (yeni token almak için)
    String tokenType,         // "Bearer"
    long accessExpiresIn,     // Saniye cinsinden access token süresi
    long refreshExpiresIn,    // Saniye cinsinden refresh token süresi
    UserResponse user         // Kullanıcı bilgileri
) {
    // Yardımcı constructor — tokenType otomatik "Bearer"
    public TokenResponse(String accessToken, String refreshToken,
                         long accessExpiresIn, long refreshExpiresIn,
                         UserResponse user) {
        this(accessToken, refreshToken, "Bearer", accessExpiresIn, refreshExpiresIn, user);
    }
}
