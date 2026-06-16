package com.dockerdemo;

// Spring Boot otomatik yapılandırma
import org.springframework.boot.SpringApplication;
// @SpringBootApplication birleşik anotasyonu
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Cache desteği
import org.springframework.cache.annotation.EnableCaching;

/**
 * Docker Advanced Demo - Ana Uygulama Sınıfı
 *
 * Bu uygulama Docker ileri seviye konuları göstermek için tasarlanmıştır:
 * - Multi-stage build ile küçük imaj boyutu
 * - Non-root user ile güvenli çalışma
 * - Nginx arkasında load balancing
 * - Container instance bilgisi (hangi konteynerde çalışıyorum?)
 * - Redis ile paylaşımlı sayaç (load balancing kanıtı)
 * - Health check entegrasyonu
 * - Environment variables yönetimi
 */
@SpringBootApplication // @Configuration + @EnableAutoConfiguration + @ComponentScan
@EnableCaching         // Redis cache desteği
public class DockerDemoApplication {

    /**
     * Uygulama başlangıç noktası.
     * Virtual threads uygulama.yml'de etkinleştirilmiştir.
     *
     * @param args komut satırı argümanları
     */
    public static void main(String[] args) {
        // Spring uygulamasını başlat
        SpringApplication.run(DockerDemoApplication.class, args);

        // Container başlangıç bilgisi - hangi instance olduğunu gösterir
        System.out.println("""

                ╔══════════════════════════════════════════════════╗
                ║      Docker Advanced Demo - Başlatıldı!          ║
                ╠══════════════════════════════════════════════════╣
                ║  Instance : %s
                ║  Hostname : %s
                ║  API      : http://localhost:8080               ║
                ║  Health   : http://localhost:8080/actuator/health║
                ╚══════════════════════════════════════════════════╝
                """.formatted(
                // INSTANCE_NAME ortam değişkeni - hangi instance olduğu (app1 veya app2)
                System.getenv().getOrDefault("INSTANCE_NAME", "bilinmiyor"),
                // HOSTNAME sistem değişkeni - Docker container ID/adı
                System.getenv().getOrDefault("HOSTNAME", "localhost")
        ));
    }
}
