package com.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * REFRESH TOKEN ENTITY — Veritabanında saklanır
 * ===============================================
 * Refresh token hem DB'de hem Redis'te tutulabilir.
 * DB: kalıcı kayıt, hangi cihazdan giriş yapıldı takibi
 * Redis: hızlı doğrulama (cache)
 *
 * TOKEN ROTATION:
 *   Her /refresh-token isteğinde yeni refresh token üretilir.
 *   Eski token invalidate edilir.
 *   Bu sayede token çalınsa bile kısa süre sonra geçersiz kalır.
 *
 * REFRESH TOKEN REUSE DETECTION:
 *   Çalınmış token kullanılırsa (zaten kullanılmış = isUsed=true):
 *   Tüm kullanıcı tokenları iptal edilir → "Token Theft" uyarısı.
 */
@Entity
@Table(name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_value", columnList = "token"),
        @Index(name = "idx_refresh_token_user",  columnList = "user_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    // Token değeri — UUID veya güçlü random string
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    // Hangi kullanıcıya ait?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Tokenin sona erme zamanı
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Daha önce kullanıldı mı? (Rotation için)
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean used = false;

    // Hangi cihaz/tarayıcıdan geldi? (İleride cihaz yönetimi için)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // IP adresi — güvenlik loglaması için
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // Token geçerli mi? (süresi dolmadı ve kullanılmadı)
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}
