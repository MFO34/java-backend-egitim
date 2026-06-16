package com.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * AUTH SERVICE — ANA SINIF
 * ========================
 * Spring Boot uygulamasının başlangıç noktası.
 *
 * @SpringBootApplication üç annotation'ın birleşimi:
 *   @Configuration     → Bu sınıf bir Spring konfigürasyon sınıfı
 *   @EnableAutoConfiguration → Spring Boot otomatik yapılandırmayı etkinleştir
 *                              (spring-boot-starter-* bağımlılıklarına göre)
 *   @ComponentScan     → com.auth paketini ve alt paketlerini tara
 *                        (@Service, @Repository, @Controller vb. bean'leri bul)
 *
 * @EnableAsync:
 *   EmailService'deki @Async metodların ayrı thread'de çalışmasını sağlar.
 *   Olmadan @Async annotation'ı yoksayılır (senkron çalışır).
 *
 * Virtual Threads (Java 21):
 *   Spring Boot 3.2+ ile virtual thread'ler destekleniyor.
 *   application.yml'de spring.threads.virtual.enabled=true ile etkinleştiriyoruz.
 *   Her HTTP isteği için ayrı virtual thread oluşturulur (platform thread değil).
 *   Yüksek concurrency'de çok daha az bellek kullanır.
 *
 *   Normal thread:  ~1MB stack → 1000 thread = 1GB RAM
 *   Virtual thread: ~few KB   → 1.000.000 thread = birkaç GB RAM
 */
@SpringBootApplication
@EnableAsync // EmailService @Async metodları için
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
