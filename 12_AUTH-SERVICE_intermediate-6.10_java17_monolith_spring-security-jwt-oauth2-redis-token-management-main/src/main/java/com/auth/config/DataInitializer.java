package com.auth.config;

import com.auth.entity.Permission;
import com.auth.entity.Role;
import com.auth.entity.User;
import com.auth.entity.AuthProvider;
import com.auth.repository.RoleRepository;
import com.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * VERİ BAŞLATICI
 * ==============
 * Uygulama başlarken varsayılan rolleri ve admin kullanıcısını oluşturur.
 *
 * ApplicationRunner nedir?
 *   Spring Boot hazır olduktan sonra (DB bağlantısı, bean'ler yüklendikten sonra) çalışır.
 *   @PostConstruct'tan farkı: tüm context yüklendikten sonra çalışır.
 *
 * Neden gerekli?
 *   - Uygulama ilk çalıştığında roller DB'de yoktur
 *   - createOAuth2User() ve AuthService.register() "USER" rolünü arar
 *   - Yoksa her seferinde oluşturur (race condition riski)
 *   - Burada bir kez oluşturup sonraki çalışmalarda var olduğunu garantileriz
 *
 * Idempotent: Zaten varsa tekrar oluşturmaz (findByName + orElseGet pattern).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Rolleri oluştur
        var userRole  = createRoleIfNotExists("USER",  getUserPermissions());
        var adminRole = createRoleIfNotExists("ADMIN", getAdminPermissions());
        var modRole   = createRoleIfNotExists("MODERATOR", getModeratorPermissions());

        // Varsayılan admin kullanıcısını oluştur
        createDefaultAdminIfNotExists(adminRole);

        log.info("Veri başlatıcı tamamlandı: USER, ADMIN, MODERATOR rolleri hazır.");
    }

    /**
     * Rol yoksa oluştur, varsa olduğu gibi döndür.
     */
    private Role createRoleIfNotExists(String name, Set<Permission> permissions) {
        return roleRepository.findByName(name)
            .orElseGet(() -> {
                var role = new Role(name);
                role.setPermissions(permissions);
                var saved = roleRepository.save(role);
                log.info("Rol oluşturuldu: {}", name);
                return saved;
            });
    }

    /**
     * Varsayılan admin kullanıcısı oluştur.
     * SADECE geliştirme ortamında. Production'da .env ile yönetilmeli.
     *
     * Kimlik bilgileri:
     *   Email:    admin@example.com
     *   Şifre:    Admin123!  (production'da değiştirin!)
     */
    private void createDefaultAdminIfNotExists(Role adminRole) {
        var adminEmail = "admin@example.com";

        if (userRepository.existsByEmail(adminEmail)) {
            return; // Zaten var, tekrar oluşturma
        }

        var admin = User.builder()
            .firstName("System")
            .lastName("Admin")
            .email(adminEmail)
            .password(passwordEncoder.encode("Admin123!")) // Şifreyi hashle
            .provider(AuthProvider.LOCAL)
            .emailVerified(true)    // Admin e-posta doğrulamasına gerek yok
            .enabled(true)
            .roles(Set.of(adminRole))
            .build();

        userRepository.save(admin);
        log.warn("⚠️  Varsayılan admin oluşturuldu: {} / Admin123! — Production'da değiştirin!", adminEmail);
    }

    // ========== PERMISSION SET'LERİ ==========

    private Set<Permission> getUserPermissions() {
        return Set.of(
            Permission.USER_READ,    // Kendi profilini görebilir
            Permission.USER_WRITE    // Kendi profilini güncelleyebilir
        );
    }

    private Set<Permission> getAdminPermissions() {
        return Set.of(
            Permission.USER_READ,
            Permission.USER_WRITE,
            Permission.USER_DELETE,
            Permission.ADMIN_READ,
            Permission.ADMIN_WRITE,
            Permission.ADMIN_DELETE
        );
    }

    private Set<Permission> getModeratorPermissions() {
        return Set.of(
            Permission.USER_READ,
            Permission.MODERATOR_READ,
            Permission.MODERATOR_WRITE
        );
    }
}
