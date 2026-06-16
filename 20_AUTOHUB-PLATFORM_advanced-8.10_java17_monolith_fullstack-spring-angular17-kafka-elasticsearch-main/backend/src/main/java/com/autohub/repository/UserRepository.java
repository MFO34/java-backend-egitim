package com.autohub.repository;

import com.autohub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Kullanıcı Repository - Spring Security için kullanıcı veri erişim katmanı
 *
 * UserDetailsService bu repository'yi kullanarak kimlik doğrulama yapar.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Kullanıcı adına göre kullanıcı bulur - Spring Security authentication için
     *
     * @param username Kullanıcı adı
     * @return Bulunan kullanıcı veya boş Optional
     */
    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    /**
     * E-posta adresine göre kullanıcı bulur - şifre sıfırlama için
     *
     * @param email E-posta adresi
     * @return Bulunan kullanıcı veya boş Optional
     */
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    /**
     * Şifre sıfırlama tokenine göre kullanıcı bulur
     *
     * @param token Şifre sıfırlama tokeni
     * @return Bulunan kullanıcı veya boş Optional
     */
    Optional<User> findByPasswordResetToken(String token);

    /**
     * Kullanıcı adının var olup olmadığını kontrol eder
     *
     * @param username Kontrol edilecek kullanıcı adı
     * @return Varsa true
     */
    boolean existsByUsername(String username);

    /**
     * E-posta adresinin var olup olmadığını kontrol eder
     *
     * @param email Kontrol edilecek email
     * @return Varsa true
     */
    boolean existsByEmail(String email);

    /**
     * Başarısız giriş sayısını artırır - güvenlik mekanizması
     *
     * @param username Kullanıcı adı
     */
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.username = :username")
    void incrementFailedAttempts(@Param("username") String username);

    /**
     * Başarılı girişte başarısız sayacı sıfırlar
     *
     * @param username Kullanıcı adı
     */
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = 0, u.accountLocked = false WHERE u.username = :username")
    void resetFailedAttempts(@Param("username") String username);
}
