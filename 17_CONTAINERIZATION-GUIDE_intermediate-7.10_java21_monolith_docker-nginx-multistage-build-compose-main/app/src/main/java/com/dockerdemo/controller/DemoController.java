package com.dockerdemo.controller;

// Spring MVC anotasyonları
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Redis template - paylaşımlı sayaç için
import org.springframework.data.redis.core.StringRedisTemplate;

// Loglama
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Zaman ve koleksiyon
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Docker Demo REST Controller
 *
 * Bu controller şu Docker kavramlarını gösterir:
 *
 * 1. LOAD BALANCING: /instance endpoint'i her seferinde farklı hostname döner
 *    → Nginx hangi instance'a yönlendirdi? (app1 mi, app2 mi?)
 *
 * 2. PAYLAŞIMLI STATE: /redis-counter Redis'teki sayacı artırır
 *    → İki instance aynı Redis'i kullanır, sayaç birleşik artarak gider
 *    → Bu load balancing'in çalıştığını kanıtlar
 *
 * 3. ORTAM DEĞİŞKENLERİ: /env endpoint'i container env variables gösterir
 *    → Docker Compose'da tanımlanan değişkenler burada görünür
 *
 * 4. SAĞLIK DURUMU: /actuator/health Spring Boot health check
 *    → Docker HEALTHCHECK direktifi bu endpoint'i kullanır
 */
@RestController // @Controller + @ResponseBody
@RequestMapping("/") // Kök path
public class DemoController {

    // Logger - istek bilgilerini kaydet
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    // application.yml'den instance adını oku (docker-compose'da INSTANCE_NAME olarak set edilir)
    @Value("${app.instance-name:bilinmiyor}")
    private String instanceAdi;

    // application.yml'den uygulama sürümünü oku
    @Value("${app.version:1.0.0}")
    private String surumu;

    // Tarih/saat biçimlendirici
    private static final DateTimeFormatter BICIM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Redis string template - paylaşımlı sayaç için
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public DemoController(StringRedisTemplate redisTemplate) {
        // Redis template bağımlılığını ata
        this.redisTemplate = redisTemplate;
    }

    // ── KÖK ENDPOINT ──────────────────────────────────────────────────────────

    /**
     * Ana endpoint - Docker ortamını karşılar.
     * GET /
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> hosgeldin() {
        // Java 21 var keyword ile LinkedHashMap (sıralı - JSON'da düzgün görünür)
        var yanit = new LinkedHashMap<String, Object>();

        // Uygulama hoş geldin mesajı
        yanit.put("mesaj", "Docker Advanced Demo'ya Hoş Geldiniz!");
        // Sürüm bilgisi
        yanit.put("surum", surumu);
        // Şimdiki zaman
        yanit.put("zaman", LocalDateTime.now().format(BICIM));

        return ResponseEntity.ok(yanit);
    }

    // ── INSTANCE BİLGİSİ (LOAD BALANCING DEMO) ──────────────────────────────

    /**
     * Container instance bilgisi döndürür.
     *
     * Nginx'in load balancing yaptığını kanıtlamak için kullanılır.
     * Bu endpoint'e birkaç kez istek atın - farklı hostname/instance
     * değerleri görüyorsanız Nginx round-robin çalışıyor demektir.
     *
     * GET /instance
     */
    @GetMapping("/instance")
    public ResponseEntity<Map<String, Object>> instanceBilgisi() {
        // Container'ın hostname'i = Docker container ID veya adı
        var hostname = System.getenv().getOrDefault("HOSTNAME", "localhost");

        // Java 21 var keyword ile yanıt haritası
        var yanit = new LinkedHashMap<String, Object>();

        // Instance adı (docker-compose'da INSTANCE_NAME olarak tanımlanır: app1 veya app2)
        yanit.put("instanceAdi", instanceAdi);
        // Sistem hostname'i = container ID/adı
        yanit.put("hostname", hostname);
        // Uygulama sürümü
        yanit.put("surum", surumu);
        // İstek zamanı
        yanit.put("zaman", LocalDateTime.now().format(BICIM));
        // Java sürümü
        yanit.put("javaSurumu", System.getProperty("java.version"));
        // OS bilgisi
        yanit.put("os", System.getProperty("os.name"));

        // Log: hangi instance bu isteği karşıladı?
        log.info("Instance bilgisi istendi: instance={}, hostname={}", instanceAdi, hostname);

        return ResponseEntity.ok(yanit);
    }

    // ── REDIS SAYAÇ (PAYLAŞIMLI STATE DEMO) ──────────────────────────────────

    /**
     * Redis'teki paylaşımlı sayacı artırır.
     *
     * İki instance (app1, app2) aynı Redis'i paylaşır.
     * Her istek sayacı 1 artırır, toplam her iki instance'tan gelen
     * isteklerin toplamını gösterir.
     *
     * Bu load balancing kanıtıdır:
     * - 10 istek at, sayaç 10 olacak
     * - Ama 5'i app1, 5'i app2 karşılamış olabilir (round-robin)
     *
     * GET /redis-counter
     */
    @GetMapping("/redis-counter")
    public ResponseEntity<Map<String, Object>> redisKontrol() {
        try {
            // Redis'teki "istek-sayaci" anahtarını 1 artır (atomik!)
            // INCR: thread-safe, tüm instance'lar güvenle artırabilir
            var sayi = redisTemplate.opsForValue().increment("istek-sayaci");

            // Instance bazlı sayacı da tut (hangi instance kaç istek karşıladı?)
            var instanceSayaci = redisTemplate.opsForValue()
                    .increment("instance-sayaci:" + instanceAdi);

            // Yanıt oluştur
            var yanit = new LinkedHashMap<String, Object>();

            // Toplam istek sayısı (tüm instance'lardan)
            yanit.put("toplamIstek", sayi);
            // Bu isteği karşılayan instance
            yanit.put("buIstemKarislayanInstance", instanceAdi);
            // Bu instance'ın toplam karşıladığı istek
            yanit.put("buInstanceninIstemSayisi", instanceSayaci);
            // Açıklama
            yanit.put("aciklama",
                    "Toplam istek tüm instance'lara dağılmıştır. " +
                    "Farklı instance'lardan gelen yanıtlar load balancing'in kanıtıdır.");

            return ResponseEntity.ok(yanit);

        } catch (Exception e) {
            // Redis bağlantı hatası
            log.error("Redis bağlantı hatası: {}", e.getMessage());
            return ResponseEntity.status(503)
                    .body(Map.of("hata", "Redis bağlantısı kurulamadı: " + e.getMessage()));
        }
    }

    // ── ORTAM DEĞİŞKENLERİ ───────────────────────────────────────────────────

    /**
     * Docker Compose'da tanımlanan güvenli ortam değişkenlerini döndürür.
     * Şifreler ve hassas bilgiler gösterilmez!
     *
     * GET /env
     */
    @GetMapping("/env")
    public ResponseEntity<Map<String, Object>> ortamDegiskenleri() {
        // Yanıt haritası
        var yanit = new LinkedHashMap<String, Object>();

        // Güvenli - şifre içermeyen değişkenleri göster
        yanit.put("INSTANCE_NAME", System.getenv().getOrDefault("INSTANCE_NAME", "-"));
        yanit.put("HOSTNAME", System.getenv().getOrDefault("HOSTNAME", "-"));
        yanit.put("SERVER_PORT", System.getenv().getOrDefault("SERVER_PORT", "8080"));
        yanit.put("DB_HOST", System.getenv().getOrDefault("DB_HOST", "-"));
        yanit.put("DB_PORT", System.getenv().getOrDefault("DB_PORT", "-"));
        yanit.put("DB_NAME", System.getenv().getOrDefault("DB_NAME", "-"));
        yanit.put("REDIS_HOST", System.getenv().getOrDefault("REDIS_HOST", "-"));
        yanit.put("REDIS_PORT", System.getenv().getOrDefault("REDIS_PORT", "-"));
        yanit.put("SPRING_PROFILES_ACTIVE", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "-"));

        // GÜVENLİK NOTU: Şifreler asla gösterilmez
        yanit.put("DB_PASSWORD", "***GIZLI***");
        yanit.put("REDIS_PASSWORD", "***GIZLI***");

        return ResponseEntity.ok(yanit);
    }

    // ── YÜK TESTİ SİMÜLASYONU ────────────────────────────────────────────────

    /**
     * Yapay gecikme ile yük testi simülasyonu.
     * Load balancer'ın yavaş instance'ı bypass edip etmediğini test eder.
     *
     * GET /slow?ms=500
     */
    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> yavasIslem(
            @RequestParam(defaultValue = "1000") long ms) throws InterruptedException {

        // Belirtilen süre kadar bekle (yapay yük simülasyonu)
        Thread.sleep(Math.min(ms, 5000)); // Maksimum 5 saniye

        // Yanıt
        var yanit = new LinkedHashMap<String, Object>();
        yanit.put("instance", instanceAdi);                 // Hangi instance
        yanit.put("beklemenSuresiMs", ms);                  // Kaç ms beklendi
        yanit.put("tamamlananZaman", LocalDateTime.now().format(BICIM)); // Bitiş zamanı

        return ResponseEntity.ok(yanit);
    }

    // ── SİSTEM BİLGİSİ ───────────────────────────────────────────────────────

    /**
     * Container sistem bilgilerini döndürür.
     * JVM bellek kullanımı, thread sayısı vb.
     *
     * GET /system
     */
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> sistemBilgisi() {
        // Runtime bilgisi - JVM'nin sistem kaynaklarına erişimi
        var runtime = Runtime.getRuntime();

        // Yanıt haritası
        var yanit = new LinkedHashMap<String, Object>();

        // Instance ve container bilgisi
        yanit.put("instanceAdi", instanceAdi);
        yanit.put("hostname", System.getenv().getOrDefault("HOSTNAME", "localhost"));

        // JVM bellek kullanımı (byte → MB dönüşümü)
        yanit.put("jvmToplamBellekMB", runtime.totalMemory() / 1024 / 1024);
        yanit.put("jvmKullanilabilirBellekMB", runtime.freeMemory() / 1024 / 1024);
        yanit.put("jvmMaksimumBellekMB", runtime.maxMemory() / 1024 / 1024);

        // CPU çekirdek sayısı
        yanit.put("cpuCekirdek", runtime.availableProcessors());

        // Aktif thread sayısı
        yanit.put("aktifThread", Thread.activeCount());

        // Java sürümü
        yanit.put("javaSurumu", System.getProperty("java.version"));

        return ResponseEntity.ok(yanit);
    }
}
