package com.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * KULLANICI ENTITY — UserDetails implementasyonu
 * ================================================
 * Spring Security'nin UserDetails arayüzünü implement eder.
 * Bu sayede User nesnesi doğrudan Authentication olarak kullanılabilir.
 *
 * UserDetails nedir?
 *   Spring Security'nin kullanıcı bilgisini temsil eden standart arayüzü.
 *   Authentication nesnesi içinde taşınır.
 *   SecurityContextHolder'a konur.
 *
 * OAuth2 Kullanıcıları:
 *   password = null (OAuth2'de şifre yoktur)
 *   provider = GOOGLE veya GITHUB
 *   providerId = Google'ın "sub" veya GitHub'ın "id" değeri
 */
@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_users_email",    columnList = "email"),
        @Index(name = "idx_users_provider", columnList = "provider,provider_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    // Benzersiz e-posta — kimlik doğrulamada username olarak kullanılır
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt ile hashlenen şifre.
     * OAuth2 kullanıcıları için NULL — şifre yoktur.
     * "$2a$10$..." formatında 60 karakter.
     */
    @Column(length = 100)
    private String password;

    // Profil resmi — OAuth2'den alınır veya kullanıcı yükler
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    /**
     * @Enumerated(STRING): "LOCAL", "GOOGLE", "GITHUB" olarak saklanır.
     * LOCAL kullanıcı sonradan Google ile giriş yapamaz (farklı kayıt akışı).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    // Google'ın "sub" değeri veya GitHub'ın "id" değeri
    @Column(name = "provider_id", length = 255)
    private String providerId;

    // E-posta doğrulama — LOCAL kullanıcılar email onaylamalı
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    // Hesap etkin mi? Admin tarafından devre dışı bırakılabilir.
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // Hesap kilitli mi? Çok fazla başarısız giriş denemesinde kilitlenir.
    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private boolean accountLocked = false;

    // Başarısız giriş denemesi sayısı
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    /**
     * ROLLER — @ManyToMany
     * Bir kullanıcının birden fazla rolü olabilir.
     * EAGER: Kullanıcı yüklenince roller de yüklenir (getAuthorities() için gerekli).
     * FetchType.EAGER burada gerekli — UserDetailsService her istek için çağrılır.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ===== UserDetails ARAYÜZ METODLARI =====

    /**
     * getAuthorities() — Spring Security'nin izin listesi
     * =====================================================
     * Hem rolleri hem de role'lerin izinlerini döndürür:
     *
     *   ROLE_ADMIN → GrantedAuthority ("ROLE_ADMIN")
     *   admin:read → GrantedAuthority ("admin:read")
     *   admin:write → GrantedAuthority ("admin:write")
     *
     * hasRole("ADMIN")        → "ROLE_ADMIN" arar
     * hasAuthority("admin:read") → "admin:read" arar
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        var authorities = new ArrayList<GrantedAuthority>();

        roles.forEach(role -> {
            // Rolü ekle: "ROLE_ADMIN"
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

            // Rolün tüm izinlerini ekle: "admin:read", "admin:write"
            role.getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getPermission()))
            );
        });

        return authorities;
    }

    /**
     * getUsername() — Spring Security'nin kimlik tanımlayıcısı
     * E-posta adresi kullanıcı adı olarak kullanılır.
     */
    @Override
    public String getUsername() {
        return email;
    }

    // Hesap süresi dolmadı mı? Basit uygulamada her zaman true.
    @Override
    public boolean isAccountNonExpired() { return true; }

    // Hesap kilitli mi değil mi?
    @Override
    public boolean isAccountNonLocked() { return !accountLocked; }

    // Şifre süresi dolmadı mı? Basit uygulamada her zaman true.
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * isEnabled() — Hesap aktif ve e-posta doğrulanmış mı?
     * LOCAL kullanıcılar: emailVerified = true olmalı.
     * OAuth2 kullanıcıları: Google/GitHub sağlayıcı doğruladı → emailVerified=true set edilir.
     */
    @Override
    public boolean isEnabled() { return enabled && (emailVerified || provider != AuthProvider.LOCAL); }

    // Yardımcı metod: tam ad
    public String getFullName() { return firstName + " " + lastName; }

    @Override
    public String toString() {
        return "User{id=" + getId() + ", email='" + email + "', provider=" + provider + "}";
    }
}
