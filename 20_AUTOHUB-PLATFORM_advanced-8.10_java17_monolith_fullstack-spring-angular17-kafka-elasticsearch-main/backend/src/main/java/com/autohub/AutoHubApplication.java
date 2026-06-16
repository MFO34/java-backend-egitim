package com.autohub;

// Spring Boot uygulamasını başlatmak için gerekli importlar
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * AutoHub Uygulamasının Ana Giriş Noktası
 *
 * Bu sınıf Spring Boot uygulamasını başlatır ve tüm konfigürasyonları aktif eder.
 * OOP Prensibi: Bu sınıf Spring'in auto-configuration mekanizmasını kullanarak
 * dependency injection container'ını başlatır.
 *
 * @SpringBootApplication anotasyonu aşağıdakileri bir arada içerir:
 * - @Configuration: Bu sınıfın Spring bean tanımları içerdiğini belirtir
 * - @EnableAutoConfiguration: Spring'in otomatik yapılandırmasını aktif eder
 * - @ComponentScan: com.autohub paketi altındaki tüm bileşenleri tarar
 */
@SpringBootApplication
// Redis önbellekleme mekanizmasını aktif eder - @Cacheable anotasyonları çalışsın diye
@EnableCaching
// Asenkron metot çalıştırmayı aktif eder - @Async anotasyonlu metotlar thread havuzunda çalışır
@EnableAsync
// Zamanlanmış görevleri aktif eder - @Scheduled anotasyonlu metotlar çalışır
@EnableScheduling
// JPA Entity'lerinde otomatik tarih/zaman alanlarını aktif eder (createdAt, updatedAt vs.)
@EnableJpaAuditing
public class AutoHubApplication {

    /**
     * Uygulamanın ana başlangıç metodu
     * Spring IoC container'ı bu metot ile başlatılır ve tüm bean'lar yüklenir.
     *
     * @param args Komut satırı argümanları - Spring'e özel parametreler geçirilebilir
     */
    public static void main(String[] args) {
        // Spring Boot uygulamasını başlat - tüm konfigürasyonlar ve bean'lar yüklenir
        SpringApplication.run(AutoHubApplication.class, args);

        // Uygulama başarıyla başladıktan sonra konsola bilgi mesajı yaz
        System.out.println("=================================================");
        System.out.println("  AutoHub - Otomotiv Yönetim Sistemi BAŞLADI!");
        System.out.println("  API: http://localhost:8080/api");
        System.out.println("  Swagger: http://localhost:8080/api/swagger-ui.html");
        System.out.println("=================================================");
    }
}
