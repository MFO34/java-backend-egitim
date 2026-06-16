package com.autohub.entity;

// Spring Security ve JPA importları
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sistem Kullanıcısı Entity Sınıfı
 *
 * Spring Security'nin UserDetails arayüzünü implemente eder.
 * Bu, kullanıcı kimlik doğrulaması için gereklidir.
 *
 * OOP Prensibi - POLİMORFİZM (Polymorphism):
 * UserDetails arayüzünü implemente ederek Spring Security ile polimorfik çalışır.
 * Spring Security bu sınıfı UserDetails olarak kullanabilir.
 *
 * OOP Prensibi - KAPSÜLLEME (Encapsulation):
 * Şifre gibi hassas bilgiler private tutulur, Spring Security yönetir.
 *
 * OOP Prensibi - SOYUTLAMA (Abstraction):
 * UserDetails arayüzü, kimlik doğrulama detaylarını soyutlar.
 */
@Entity
@Table(name = "users",
        indexes = {
            // Kullanıcı adı benzersiz olmalı - login için kullanılır
            @Index(name = "idx_user_username", columnList = "username", unique = true),
            // E-posta benzersiz olmalı
            @Index(name = "idx_user_email", columnList = "email", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "roles"})  // Şifreyi toString'e dahil etme (güvenlik)
public class User extends BaseEntity implements UserDetails {

    /**
     * Kullanıcı adı - sisteme giriş için kullanılır
     */
    @NotBlank(message = "Kullanıcı adı zorunludur")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arasında olmalıdır")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * E-posta adresi - şifre sıfırlama ve bildirimler için
     */
    @NotBlank(message = "E-posta zorunludur")
    @Email(message = "Geçersiz e-posta formatı")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Şifrelenmiş parola - BCrypt ile hash'lenmiş olarak saklanır
     * Hiçbir zaman düz metin şifre saklanmaz (güvenlik prensibi)
     */
    @NotBlank(message = "Şifre zorunludur")
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Kullanıcının adı
     */
    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    /**
     * Kullanıcının soyadı
     */
    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    /**
     * Kullanıcıya atanmış roller
     * Many-to-Many ilişkisi: Bir kullanıcı birden fazla role sahip olabilir
     */
    @ManyToMany(fetch = FetchType.EAGER)  // Roller her zaman yüklensin
    @JoinTable(
        name = "user_roles",              // Ara tablo adı
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"})
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Hesap kilitlenme durumu - başarısız giriş denemelerinde kilitlenir
     */
    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    /**
     * Hesabın süresi dolmuş mu - belirli periyotlarda yenilenmesi gerekebilir
     */
    @Column(name = "account_expired", nullable = false)
    @Builder.Default
    private Boolean accountExpired = false;

    /**
     * Kimlik bilgilerinin süresi dolmuş mu - şifre yenileme zorunluluğu
     */
    @Column(name = "credentials_expired", nullable = false)
    @Builder.Default
    private Boolean credentialsExpired = false;

    /**
     * Başarısız giriş denemesi sayısı - 5 deneme sonrası hesap kilitlenir
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * Son başarılı giriş zamanı - güvenlik izleme için
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Şifre sıfırlama tokeni - şifre unutulduğunda üretilir
     */
    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    /**
     * Şifre sıfırlama tokeninin geçerlilik süresi
     */
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // ==========================================================
    // Spring Security UserDetails arayüz metodlarının implementasyonu
    // ==========================================================

    /**
     * Kullanıcının tüm yetkilerini döndürür
     * Spring Security bu metodu kullanarak yetkilendirme kararları verir
     *
     * @return GrantedAuthority listesi - her rol bir yetki olarak döner
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Her role'ü SimpleGrantedAuthority'ye dönüştür
        // "ROLE_" prefix'i Spring Security konvansiyonu gereğidir
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Hesabın süresi dolmuş mu kontrolü
     * Spring Security bu metodu kimlik doğrulama sırasında çağırır
     */
    @Override
    public boolean isAccountNonExpired() {
        // accountExpired false ise hesap geçerli, true ise süresi dolmuş
        return !accountExpired;
    }

    /**
     * Hesap kilitli mi kontrolü
     * 5 başarısız giriş denemesinden sonra hesap kilitlenir
     */
    @Override
    public boolean isAccountNonLocked() {
        // accountLocked false ise hesap açık, true ise kilitli
        return !accountLocked;
    }

    /**
     * Kimlik bilgilerinin geçerliliği kontrolü
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return !credentialsExpired;
    }

    /**
     * Hesap aktif mi kontrolü - BaseEntity'den gelen isActive alanını kullanır
     */
    @Override
    public boolean isEnabled() {
        // isActive ve isDeleted durumlarını kontrol et
        return Boolean.TRUE.equals(this.getIsActive()) &&
               Boolean.FALSE.equals(this.getIsDeleted());
    }

    /**
     * Başarısız giriş denemesini kaydeder ve 5 deneme sonrası hesabı kilitler
     */
    public void incrementFailedAttempts() {
        // Başarısız deneme sayısını artır
        this.failedAttempts++;
        // 5 veya daha fazla başarısız deneme varsa hesabı kilitle
        if (this.failedAttempts >= 5) {
            this.accountLocked = true;
        }
    }

    /**
     * Başarılı girişte başarısız deneme sayısını sıfırlar
     */
    public void resetFailedAttempts() {
        // Başarısız deneme sayısını sıfırla
        this.failedAttempts = 0;
        // Hesabın kilidini aç
        this.accountLocked = false;
        // Son giriş zamanını güncelle
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Kullanıcının tam adını döndürür
     */
    public String getFullName() {
        // firstName veya lastName null ise kullanıcı adını döndür
        if (firstName == null || lastName == null) return username;
        return firstName + " " + lastName;
    }
}
