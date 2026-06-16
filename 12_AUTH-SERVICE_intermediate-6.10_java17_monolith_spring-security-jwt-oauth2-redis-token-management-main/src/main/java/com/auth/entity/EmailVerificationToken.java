package com.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * E-POSTA DOĞRULAMA TOKEN — 24 saat geçerli
 * ============================================
 * Kayıt akışı:
 *   1. Kullanıcı kayıt olur (emailVerified=false)
 *   2. Bu token oluşturulur ve e-posta ile gönderilir
 *   3. Kullanıcı linke tıklar: GET /auth/verify-email?token=xxx
 *   4. Token doğrulanır → emailVerified=true
 */
@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken extends BaseEntity {

    // UUID token — e-postadaki link bu değeri içerir
    @Column(nullable = false, unique = true, length = 200)
    private String token;

    // Hangi kullanıcıya ait?
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 24 saat geçerli
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Kullanıldı mı?
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean used = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
