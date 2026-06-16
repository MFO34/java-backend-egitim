package com.eticaret;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * E-TİCARET API — GİRİŞ NOKTASI
 * =================================
 * @SpringBootApplication:
 *   @Configuration     → Spring konfigürasyonu
 *   @EnableAutoConfiguration → Classpath'e göre otomatik yapılandırma
 *   @ComponentScan     → com.eticaret altındaki tüm bean'leri tara
 *
 * Uygulama başlarken:
 *   1. Spring IoC Container başlar
 *   2. Liquibase migration çalışır (001, 002 changeset'leri)
 *   3. JPA @EnableJpaAuditing aktive olur
 *   4. Virtual Thread Executor Tomcat'e kurulur
 *   5. Port 8080'de API hazır
 */
@SpringBootApplication
public class EticaretApplication {

    public static void main(String[] args) {
        SpringApplication.run(EticaretApplication.class, args);
    }
}
