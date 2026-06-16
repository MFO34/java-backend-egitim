package com.auth.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * İZİN (PERMISSION) ENUMu — İnce Taneli Yetkilendirme
 * =====================================================
 * ROLE vs AUTHORITY (Permission) farkı:
 *
 *   ROLE  → Geniş kapsamlı gruplama (ADMIN, USER, MODERATOR)
 *           Spring Security'de "ROLE_" ön ekiyle saklanır.
 *           hasRole("ADMIN") → "ROLE_ADMIN" arar.
 *
 *   AUTHORITY (Permission) → İnce taneli eylem bazlı izin
 *           "user:read", "admin:write" gibi spesifik izinler.
 *           hasAuthority("user:read") ile kontrol edilir.
 *           "ROLE_" ön eki YOKTUR.
 *
 * Gerçek dünya örneği:
 *   ROLE_ADMIN     → user:read, user:write, user:delete, admin:read, admin:write
 *   ROLE_MODERATOR → user:read, user:write
 *   ROLE_USER      → (sadece kendi profili)
 */
@Getter
@RequiredArgsConstructor
public enum Permission {

    // ===== KULLANICI İZİNLERİ =====
    USER_READ("user:read"),       // Kullanıcı bilgilerini okuma
    USER_WRITE("user:write"),     // Kullanıcı bilgilerini güncelleme
    USER_DELETE("user:delete"),   // Kullanıcı silme

    // ===== YÖNETİCİ İZİNLERİ =====
    ADMIN_READ("admin:read"),     // Admin paneli okuma
    ADMIN_WRITE("admin:write"),   // Admin paneli yazma
    ADMIN_DELETE("admin:delete"), // Admin paneli silme

    // ===== MODERATÖR İZİNLERİ =====
    MODERATOR_READ("mod:read"),   // Moderasyon okuma
    MODERATOR_WRITE("mod:write"); // Moderasyon yazma

    // Spring Security GrantedAuthority'de kullanılacak string
    private final String permission;
}
