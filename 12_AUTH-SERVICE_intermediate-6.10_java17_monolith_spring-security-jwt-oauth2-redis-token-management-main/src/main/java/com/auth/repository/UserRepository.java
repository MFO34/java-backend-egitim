package com.auth.repository;

import com.auth.entity.AuthProvider;
import com.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // E-postaya göre kullanıcı bul — login ve kayıt kontrolü için
    Optional<User> findByEmail(String email);

    // Bu e-posta kayıtlı mı?
    boolean existsByEmail(String email);

    // OAuth2 sağlayıcı + sağlayıcı ID ile bul (Google sub, GitHub id)
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    // Tüm aktif kullanıcılar (admin paneli)
    Page<User> findByEnabledTrue(Pageable pageable);

    // Başarısız giriş sayısını artır
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(@Param("id") Long id);

    // Başarılı girişte sayacı sıfırla
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLocked = false WHERE u.id = :id")
    void resetFailedAttempts(@Param("id") Long id);

    // Hesabı kilitle
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true WHERE u.id = :id")
    void lockAccount(@Param("id") Long id);
}
