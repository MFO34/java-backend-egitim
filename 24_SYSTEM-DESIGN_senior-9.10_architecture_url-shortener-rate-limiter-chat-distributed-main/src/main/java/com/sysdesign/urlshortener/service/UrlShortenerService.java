package com.sysdesign.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * URL KISALTMA SERVİSİ — Sistem Tasarımı Örneği
 * ================================================
 *
 * Gereksinimler (bit.ly ölçeği):
 *   - 100M URL/gün yazma → ~1.160 yazma/saniye
 *   - 1B URL/gün okuma  → ~11.600 okuma/saniye (10:1 okuma/yazma oranı)
 *   - 7 karakter kısa kod
 *   - TTL desteği (geçici linkler)
 *   - Analytics (tıklama sayısı)
 *
 * Kısa kod üretim yöntemleri karşılaştırması:
 *
 *   1. Hash (MD5/SHA):
 *      MD5(originalUrl) → 128 bit → Base62 encode → ilk 7 karakter.
 *      Problem: Farklı URL'ler aynı hash → çakışma (collision) riski.
 *      Çakışma: Yeniden hash üret (rehash) → mantık karmaşıklaşır.
 *      Avantaj: Aynı URL her seferinde aynı kodu verir (idempotent).
 *
 *   2. Sayaç + Base62 Encode (bu implementasyon):
 *      Artan sayaç → toBase62() → benzersiz kod.
 *      Avantaj: Çakışma yok — her sayaç değeri benzersiz.
 *      Problem: Tahmin edilebilir (predictable) — 1, 2, 3... → URL enumeration saldırısı.
 *      Çözüm: Başlangıç değerini rastgele seç veya Snowflake ID kullan.
 *
 *   3. Snowflake ID + Base62 (önerilen production yöntemi):
 *      Zaman damgası + makine ID + sıra numarası → 64 bit benzersiz ID.
 *      Dağıtık: Her sunucu kendi ID'sini üretir, koordinasyon gerekmez.
 *      Tahmin edilemez: Zaman + makine bileşimi.
 *
 * Base62 neden?
 *   Alfanumerik: 0-9 + A-Z + a-z = 62 karakter.
 *   URL-safe: Özel karakter yok (Base64'teki +, / gibi URL encode sorunu yok).
 *   62^7 = 3.521.614.606.208 (~3.5 trilyon) benzersiz URL → yıllarca yeter.
 *
 * Scale: Bu implementasyon tek instance, gerçekte:
 *   Okuma: Redis cache (99% hit rate) → DB'ye nadiren gider.
 *   Yazma: Kafka queue → async DB write (yük dengeleme).
 *   Coğrafi: Multi-region DNS → en yakın bölgeye yönlendir.
 */
@Service
public class UrlShortenerService {

    // Base62 alfabe: sayısal + büyük harf + küçük harf (62 karakter)
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 7; // 62^7 = 3.5T benzersiz URL

    // In-memory store: Gerçekte Redis (L1 cache) + PostgreSQL (kalıcı store)
    private final Map<String, String> store = new ConcurrentHashMap<>();

    // Click analytics: Gerçekte Redis HyperLogLog (unique count) veya Kafka event
    private final Map<String, Long> clickCount = new ConcurrentHashMap<>();

    // AtomicLong: Thread-safe artan sayaç — eş zamanlı isteklerde race condition yok
    // 10_000_000: Başlangıç → kısa kodlar 0000001'den değil orta değerden başlar (daha az tahmin edilebilir)
    // Gerçekte: Redis INCR veya DB sequence (dağıtık, restart-proof)
    private final AtomicLong counter = new AtomicLong(10_000_000L);

    private final StringRedisTemplate redisTemplate;

    public UrlShortenerService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * URL kısaltma — shorten().
     *
     * Adımlar:
     *   1. Deduplication: Aynı URL daha önce kısaltıldıysa → mevcut kodu döndür.
     *      Neden? Aynı URL için N farklı kısa kod: Veritabanı şişer, kullanıcı kafa karışır.
     *      Gerçekte: DB index ile verimli lookup (O(log n)), burada stream (demo için).
     *
     *   2. Sayaç artır → Base62 encode → benzersiz kod üret.
     *      AtomicLong.incrementAndGet(): Thread-safe — iki thread aynı değeri alamaz.
     *      Gerçekte: DB sequence veya Redis INCR (restart-proof, dağıtık).
     *
     *   3. In-memory + Redis'e kaydet.
     *      In-memory: Bu demo için — restart olunca sıfırlanır.
     *      Redis: TTL ile → süre dolunca otomatik sil.
     *      Gerçekte: PostgreSQL'e de yaz (kalıcı), Redis sadece cache.
     *
     * TTL neden?
     *   Geçici kampanya linki: 30 gün sonra otomatik sil.
     *   Depolama tasarrufu: Kullanılmayan URL'ler birikmesin.
     *   Gerçekte: Soft delete (is_active=false) tercih edilebilir — link geçmişi izlenebilir.
     */
    public String shorten(String originalUrl, Duration ttl) {
        // 1. Deduplication: Aynı URL'ye aynı kodu ver
        Optional<String> existing = store.entrySet().stream()
                .filter(e -> e.getValue().equals(originalUrl))
                .map(Map.Entry::getKey)
                .findFirst();
        if (existing.isPresent()) return existing.get();

        // 2. Atomik sayaç artır → Base62 encode
        long id = counter.incrementAndGet(); // Thread-safe: iki thread asla aynı id almaz
        String code = toBase62(id);

        // 3. Kaydet (in-memory + Redis)
        store.put(code, originalUrl);
        clickCount.put(code, 0L);

        // Redis: TTL ile cache → süre dolunca otomatik sil
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.opsForValue().set("url:" + code, originalUrl, ttl);
        }

        return code;
    }

    /**
     * URL çözümleme — resolve() — Cache-Aside Pattern.
     *
     * Cache-Aside (Önbellekten Okuma) nedir?
     *   1. Önce Redis'i kontrol et (cache).
     *   2. Cache hit: Doğrudan döndür — DB'ye gitme.
     *   3. Cache miss: DB'den oku → döndür (Redis'e tekrar yaz — warm-up).
     *
     * Neden okuma ağırlıklı sistemde kritik?
     *   1B istek/gün → her biri DB'ye giderse: DB ezilir.
     *   Redis hit rate %99 olursa: DB yükü 100x azalır.
     *   Gerçekte: DB'den okunan veri Redis'e yazılır (warm-up) → sonraki okuma cache'den.
     *
     * incrementClick: Her resolve'da tıklama sayacını artır (analytics).
     *   ConcurrentHashMap.merge: Atomic operasyon — race condition yok.
     *   Gerçekte: Kafka event ("url.clicked") → async analytics pipeline.
     *   Senkron sayma: Her istek için DB write → yavaş, gereksiz.
     */
    public Optional<String> resolve(String code) {
        // 1. Cache-Aside: Redis'i kontrol et
        try {
            String cached = redisTemplate.opsForValue().get("url:" + code);
            if (cached != null) {
                incrementClick(code); // Analytics: tıklama sayacı
                return Optional.of(cached); // Cache hit — DB'ye gitme
            }
        } catch (Exception ignored) {
            // Redis bağlantı hatası → in-memory'e devam et (graceful degradation)
        }

        // 2. Cache miss: In-memory'den oku (gerçekte DB)
        String url = store.get(code);
        if (url != null) {
            incrementClick(code);
            // Gerçekte: Redis'e tekrar yaz → warm-up (sonraki okuma cache'den gelsin)
        }
        return Optional.ofNullable(url);
    }

    public long getClickCount(String code) {
        return clickCount.getOrDefault(code, 0L);
    }

    /**
     * ConcurrentHashMap.merge — Thread-Safe Sayaç Artırma.
     *
     * clickCount.merge(code, 1L, Long::sum):
     *   Eğer key yoksa: 1L değeriyle ekle.
     *   Eğer key varsa: Long::sum ile mevcut değere 1 ekle.
     *   Thread-safe: ConcurrentHashMap.merge atomic — race condition yok.
     *   Alternatif yanlış yöntem: get() → +1 → put() → race condition var!
     *
     * Gerçekte: Redis INCR komutu — atomic ve dağıtık.
     *   redisTemplate.opsForValue().increment("click:" + code)
     */
    private void incrementClick(String code) {
        clickCount.merge(code, 1L, Long::sum); // Atomic increment
    }

    /**
     * Base62 Encoding — Sayıyı 7 Karakterli Koda Dönüştür.
     *
     * Algoritma:
     *   Sayıyı 62 tabanına çevir (ondalık → 62'lik sistem).
     *   Her basamak: BASE62 dizisinden karakter al.
     *   Tersten yaz → doğru sıra.
     *   7 karaktere tamamla (sıfır doldurma).
     *
     * Örnek: id = 10_000_001
     *   10_000_001 mod 62 = 45 → BASE62[45] = 'j'
     *   10_000_001 / 62  = 161290
     *   161290 mod 62    = 14 → BASE62[14] = 'E'
     *   ... → "0002FEj" gibi 7 karakter
     *
     * substring(0, 7):
     *   Küçük sayılar 7 karakterden kısa olabilir → '0' ile doldur.
     *   Büyük sayılar 7 karakterden uzun olabilir → ilk 7 karakter al.
     *   Bu kesme: Potansiyel çakışma — gerçekte doğru uzunluk hesaplanmalı.
     */
    public String toBase62(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62.charAt((int)(num % 62))); // Her basamak: 62'lik sistemde karakter
            num /= 62;
        }
        while (sb.length() < CODE_LENGTH) sb.append('0'); // Kısa ise sola '0' doldur
        return sb.reverse().toString().substring(0, CODE_LENGTH);
    }

    /**
     * Base62 Decoding — Kodu Sayıya Çevir.
     *
     * Encoding'in tersi: Pozisyonel sayı sistemi.
     *   "0002FEj" → 0×62^6 + 0×62^5 + ... + j×62^0
     *   BASE62.indexOf(c): Karakterin 62 tabanlı değeri (0-61).
     *   Kullanım: Analytics, debug, veya ID'den orijinal URL'yi bulmak için.
     */
    public long fromBase62(String code) {
        long result = 0;
        for (char c : code.toCharArray()) {
            result = result * 62 + BASE62.indexOf(c);
        }
        return result;
    }
}
