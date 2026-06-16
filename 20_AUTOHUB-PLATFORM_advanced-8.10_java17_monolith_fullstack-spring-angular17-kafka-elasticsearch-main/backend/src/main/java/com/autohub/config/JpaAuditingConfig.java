package com.autohub.config;

// Spring importları
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing Konfigürasyonu
 *
 * BaseEntity'deki createdBy ve updatedBy alanlarını otomatik doldurmak için
 * Security context'ten mevcut kullanıcıyı alır.
 *
 * Bu konfigürasyon ile kim ne zaman kayıt oluşturdu/güncelledi bilgisi
 * otomatik olarak tutulur - audit trail için kritik.
 */
@Configuration
public class JpaAuditingConfig {

    /**
     * Mevcut kimlik doğrulanmış kullanıcıyı döndürür
     * JPA Auditing bu bean'ı kullanarak createdBy ve updatedBy alanlarını doldurur
     *
     * @return AuditorAware - geçerli kullanıcıyı sağlayan bean
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        // Lambda ile AuditorAware implementasyonu
        return () -> {
            // Spring Security context'inden authentication al
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Kimlik doğrulanmamış istek (anonim kullanıcı)
            if (authentication == null || !authentication.isAuthenticated()) {
                // Sistem işlemleri veya anonim istekler için "system" kullan
                return Optional.of("system");
            }

            // Kimlik doğrulanmış kullanıcının adını döndür
            return Optional.of(authentication.getName());
        };
    }
}
