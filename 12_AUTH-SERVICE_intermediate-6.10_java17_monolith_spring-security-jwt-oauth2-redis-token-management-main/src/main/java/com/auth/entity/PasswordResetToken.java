package com.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ŞİFRE SIFIRLAMA TOKEN — 1 saat geçerli
 * =========================================
 * Forgot password akışı:
 *   1. POST /auth/forgot-password → e-posta gönderilir
 *   2. Kullanıcı linke tıklar → frontend şifre sıfırlama formu gösterir
 *   3. POST /auth/reset-password {token, newPassword}
 *   4. Token doğrulanır → şifre güncellenir → token kullanıldı işaretlenir
 *
 * Güvenlik önlemi:
 *   Token kullanıldıktan sonra hemen geçersiz kılınır.
 *   Brute force için rate limiting eklenebilir (Redis ile).
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 200)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 1 saat geçerli (emailVerification'dan kısa — şifre sıfırlama daha kritik)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean used = false;

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }

    public boolean isValid() { return !used && !isExpired(); }
}
