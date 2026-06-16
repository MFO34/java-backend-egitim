package com.auth.repository;

import com.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Kullanıcının tüm aktif tokenlarını bul (cihaz yönetimi)
    @Query("SELECT r FROM RefreshToken r WHERE r.user.id = :userId AND r.used = false")
    java.util.List<RefreshToken> findActiveTokensByUserId(@Param("userId") Long userId);

    // Kullanıcının tüm refresh tokenlarını geçersiz kıl (logout all devices)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.used = true WHERE r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    // Süresi dolmuş tokenları temizle (scheduled task için)
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
