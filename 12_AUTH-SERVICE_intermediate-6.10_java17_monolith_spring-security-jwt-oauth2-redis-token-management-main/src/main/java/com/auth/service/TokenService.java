package com.auth.service;

import com.auth.entity.RefreshToken;
import com.auth.entity.User;
import com.auth.repository.RefreshTokenRepository;
import com.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * TOKEN SERVİSİ
 * =============
 * İki farklı token yönetimini koordine eder:
 *
 * 1. ACCESS TOKEN BLACKLIST (Redis)
 *    - Logout'ta access token geçersiz kılınır
 *    - key:   "blacklist:jti:<uuid>"   (JTI = JWT ID, her token'a unique)
 *    - value: "revoked"
 *    - TTL:   token'ın kalan süresi (expire olunca Redis'ten otomatik silinir)
 *
 * 2. REFRESH TOKEN YÖNETİMİ (PostgreSQL)
 *    - DB'de saklanır çünkü uzun ömürlü (7 gün) ve reuse detection gerekir
 *    - Token rotation: her refresh'te yeni token üretilir, eski markedUsed
 *    - Reuse detection: kullanılmış token tekrar gelirse → tüm session'ları sonlandır
 *
 * Neden iki farklı depolama?
 *   Access token (15 dk) → Redis: hızlı, kısa TTL, otomatik temizlenir
 *   Refresh token (7 gün) → DB: kalıcı, sorgu yapılabilir, tüm token'ları görebilirsin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    // Redis operasyonları için
    private final StringRedisTemplate redisTemplate;

    // Refresh token DB işlemleri için
    private final RefreshTokenRepository refreshTokenRepository;

    // JWT parse/üretme için (TTL hesabı)
    private final JwtService jwtService;

    // Redis key prefix — diğer key'lerle çakışmayı önler
    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    // ========== ACCESS TOKEN BLACKLIST (Redis) ==========

    /**
     * Access token'ı blacklist'e ekle (logout veya token çalınma şüphesi)
     *
     * @param token JWT access token string
     */
    public void blacklistAccessToken(String token) {
        try {
            // Token'ın JTI'sını al (JWT ID — her token'a unique UUID)
            var jti = jwtService.extractJti(token);

            // Token'ın ne kadar süresi kaldı?
            var expiration = jwtService.extractExpiration(token);
            var remainingTtl = Duration.between(Instant.now(), expiration.toInstant());

            // Kalan süre pozitifse blacklist'e ekle
            if (!remainingTtl.isNegative()) {
                var redisKey = BLACKLIST_PREFIX + jti;
                // SET "blacklist:jti:<uuid>" "revoked" EX <remainingSeconds>
                redisTemplate.opsForValue().set(redisKey, "revoked", remainingTtl);
                log.debug("Access token blacklist'e eklendi: jti={}", jti);
            }
            // Süre dolmuşsa zaten geçersiz, Redis'e eklemeye gerek yok

        } catch (Exception e) {
            // Token parse edilemiyorsa (zaten geçersiz), sessizce geç
            log.warn("Access token blacklist'e eklenemedi: {}", e.getMessage());
        }
    }

    /**
     * Access token blacklist'te mi?
     *
     * @param jti JWT ID (JwtAuthFilter'da token doğrulanmadan önce kontrol edilir)
     * @return true ise token geçersiz (logout yapılmış)
     */
    public boolean isAccessTokenBlacklisted(String jti) {
        var redisKey = BLACKLIST_PREFIX + jti;
        // EXISTS komutunu simüle: key varsa true
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    // ========== REFRESH TOKEN (PostgreSQL) ==========

    /**
     * Yeni refresh token'ı DB'ye kaydet.
     * OAuth2SuccessHandler ve AuthService.login() tarafından çağrılır.
     */
    @Transactional
    public RefreshToken saveRefreshToken(User user, String tokenValue,
                                          String ipAddress, String userAgent) {
        // JWT'nin expiration'ından DB için expiresAt hesapla
        var expiration = jwtService.extractExpiration(tokenValue);

        var refreshToken = RefreshToken.builder()
            .user(user)
            .token(tokenValue)
            .expiresAt(expiration.toInstant())  // Date → Instant
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .isUsed(false)
            .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Refresh token ile yeni access + refresh token üret (Token Rotation).
     *
     * Token Rotation şeması:
     *   Client → "refresh_token=abc" gönderir
     *   Sunucu → abc'yi "kullanıldı" işaretler
     *   Sunucu → yeni access + refresh token üretir
     *   Client → yeni refresh_token'ı saklar
     *
     * Reuse Detection:
     *   abc tekrar gönderilirse → zaten kullanılmış → saldırı şüphesi!
     *   Tüm kullanıcının refresh token'ları iptal edilir.
     *
     * @param tokenValue Gelen refresh token string
     * @return Yeni token çifti (access + refresh) veya boş (geçersiz token)
     */
    @Transactional
    public Optional<TokenPair> rotateRefreshToken(String tokenValue,
                                                   String ipAddress, String userAgent) {
        // 1. DB'de bu token var mı?
        var existingOpt = refreshTokenRepository.findByToken(tokenValue);

        if (existingOpt.isEmpty()) {
            log.warn("Bilinmeyen refresh token: {}", tokenValue.substring(0, 20) + "...");
            return Optional.empty();
        }

        var existing = existingOpt.get();

        // 2. Daha önce kullanılmış mı? (REUSE DETECTION)
        if (existing.isUsed()) {
            log.error("KULLANILMIŞ REFRESH TOKEN TEKRAR KULLANILDI! userId={} → Tüm session'lar kapatılıyor",
                existing.getUser().getId());
            // Güvenlik ihlali: bu kullanıcının TÜM refresh token'larını iptal et
            refreshTokenRepository.revokeAllByUserId(existing.getUser().getId());
            return Optional.empty();
        }

        // 3. Süresi dolmuş mu?
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Süresi dolmuş refresh token: userId={}", existing.getUser().getId());
            return Optional.empty();
        }

        // 4. Eski token'ı "kullanıldı" olarak işaretle (artık geçersiz)
        existing.setUsed(true);
        refreshTokenRepository.save(existing);

        // 5. Yeni token çifti üret
        var user = existing.getUser();
        var newAccessToken  = jwtService.generateAccessToken(user);
        var newRefreshToken = jwtService.generateRefreshToken(user);

        // 6. Yeni refresh token'ı DB'ye kaydet
        saveRefreshToken(user, newRefreshToken, ipAddress, userAgent);

        log.debug("Token rotation başarılı: userId={}", user.getId());
        return Optional.of(new TokenPair(newAccessToken, newRefreshToken));
    }

    /**
     * Kullanıcının tüm refresh token'larını iptal et (logout all devices).
     */
    @Transactional
    public void revokeAllUserRefreshTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Tüm refresh token'lar iptal edildi: userId={}", userId);
    }

    /**
     * Access + Refresh token çiftini taşıyan record.
     * TokenService metodlarından döner, AuthService'e geçirilir.
     */
    public record TokenPair(String accessToken, String refreshToken) {}
}
