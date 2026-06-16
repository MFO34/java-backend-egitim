package com.eticaret.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA AUDİTİNG YAPILANDIRMASI
 * ==============================
 * @EnableJpaAuditing:
 *   @CreatedDate, @LastModifiedDate, @CreatedBy anotasyonlarını aktive eder.
 *   Bu olmadan BaseEntity'deki audit alanları doldurulmaz.
 *
 * auditorAwareRef = "auditorProvider":
 *   @CreatedBy alanını dolduracak bean'in adı.
 *   Gerçek uygulamada Spring Security'den user alınır.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * AuditorAware<String>:
     *   Spring'e "şu an kim işlem yapıyor?" sorusunu yanıtlar.
     *   Bu örnekte sabit "system" dönüyor.
     *   Gerçek uygulamada:
     *     SecurityContextHolder.getContext().getAuthentication().getName()
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        // Lambda ile AuditorAware implementasyonu
        // Optional.of("system") → createdBy = "system"
        return () -> Optional.of("system");
    }
}
