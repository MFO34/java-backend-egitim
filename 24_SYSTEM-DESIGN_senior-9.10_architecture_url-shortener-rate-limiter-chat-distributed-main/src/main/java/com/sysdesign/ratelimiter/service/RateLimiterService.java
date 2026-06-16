package com.sysdesign.ratelimiter.service;

import com.sysdesign.ratelimiter.algorithm.SlidingWindowRateLimiter;
import com.sysdesign.ratelimiter.algorithm.TokenBucketRateLimiter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * RATE LIMITER SERVİSİ — Hız Sınırlama
 * =======================================
 *
 * Rate Limiting nedir ve neden gerekli?
 *   Bir API'yi sınırsız çağırabilmek: Kaynak tüketimi (CPU/DB), DDoS saldırısı, kötüye kullanım.
 *   Rate Limiting: Belirli zaman penceresi içinde maksimum N istek.
 *   Örnek: "Kullanıcı başına dakikada 100 istek" veya "IP başına saniyede 10 istek".
 *
 * Tek JVM vs Dağıtık Rate Limiting:
 *   Tek instance (local): AtomicInteger veya ConcurrentHashMap yeterli.
 *   N instance (Kubernetes pod'ları): Her pod kendi sayacını tutar → toplam limit aşılır.
 *   Çözüm: Merkezi Redis → tüm pod'lar aynı sayacı günceller.
 *
 * Algoritmalar:
 *
 *   Token Bucket (Jeton Kovası):
 *     Sabit kapasiteli bir kova, sabit hızda jeton eklenir.
 *     Her istek 1 jeton harcar. Kovada jeton yoksa istek reddedilir.
 *     Avantaj: Burst (ani yoğunluk) toleransı — kova dolu ise burst izin verilir.
 *     Kullanım: API burst'ına izin ver ama ortalamayı kontrol et.
 *
 *   Sliding Window Counter:
 *     Son X milisaniye içindeki istek sayısını takip eder.
 *     Pencere kayarak ilerler — sabit zaman dilimleri yerine gerçek zaman.
 *     Avantaj: Sabit window'daki "pencere başında burst" sorunu yok.
 *     Kullanım: Kesin zaman penceresi gerektiğinde (örn: dakikada tam 100 istek).
 *
 * Redis ile dağıtık rate limiting:
 *   ZADD: Her istek timestamp ile sorted set'e eklenir.
 *   ZREMRANGEBYSCORE: Zaman penceresi dışındaki eski kayıtlar silinir.
 *   ZCARD: Mevcut penceredeki istek sayısı.
 *   Lua script: Tüm işlemler atomik — race condition yok.
 */
@Service
public class RateLimiterService {

    private final StringRedisTemplate redis;
    private final TokenBucketRateLimiter localTokenBucket;
    private final SlidingWindowRateLimiter localSlidingWindow;

    /**
     * Redis Lua Script — Atomik Sliding Window Counter.
     *
     * Neden Lua script? (EVAL komutu)
     *   Race condition problemi: 2 pod aynı anda sayacı okursa:
     *     Pod A: ZCARD → 99 (limitin altında) → istek kabul
     *     Pod B: ZCARD → 99 (limitin altında) → istek kabul (aynı anda!)
     *     Sonuç: 100 limit için 101 istek geçti.
     *   Lua script: Redis tek thread'de çalışır → script atomik → race condition yok.
     *   Tüm ZREMRANGEBYSCORE + ZCARD + ZADD işlemleri tek atomik blok.
     *
     * Sorted Set (ZADD) neden?
     *   Her istek timestamp'i score olarak eklenir.
     *   ZREMRANGEBYSCORE: "now - window" öncesini sil → otomatik temizlik.
     *   ZCARD: Kalan eleman sayısı = aktif penceredeki istek sayısı.
     *   Alternatif: String + INCR → sayaç basit ama eski kayıtlar temizlenemiyor.
     *
     * PEXPIRE neden?
     *   Key'e TTL set et: Pencere bittikten sonra key otomatik silinir.
     *   Temizlik gerekmez — Redis TTL mekanizması halleder.
     *   Not: PEXPIRE her istekte yenilenir → kullanıcı aktifken key yaşar.
     */
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])

            -- Zaman penceresi dışındaki eski kayıtları temizle (sliding window)
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

            -- Mevcut penceredeki istek sayısı
            local count = redis.call('ZCARD', key)

            if count < limit then
                -- Limit altındaysa: Yeni isteği kaydet ve izin ver
                redis.call('ZADD', key, now, now)      -- timestamp hem score hem member
                redis.call('PEXPIRE', key, window)     -- TTL: pencere süresi kadar
                return 1                               -- izin verildi
            else
                return 0                               -- limit aşıldı
            end
            """;

    public RateLimiterService(StringRedisTemplate redis) {
        this.redis = redis;
        // 100 burst kapasiteli, saniyede 10 jeton yenilenen token bucket
        this.localTokenBucket = new TokenBucketRateLimiter(100, 10);
        // Dakikada maksimum 100 istek (60_000ms pencere)
        this.localSlidingWindow = new SlidingWindowRateLimiter(100, 60_000L);
    }

    /**
     * Redis ile dağıtık rate limiting — Production yöntemi.
     *
     * "rl:" prefix: Anahtar namespace'i — diğer Redis key'leriyle çakışmayı önler.
     *   key = "rl:user:42" veya "rl:ip:192.168.1.1"
     *   Namespace: Büyük sistemlerde farklı servislerin key'leri çakışmaz.
     *
     * execute() parametreleri:
     *   DefaultRedisScript: Lua script + dönüş tipi (Long.class).
     *   KEYS[1]: Redis key (rate limiter için).
     *   ARGV[1]: now (milisaniye) — mevcut zaman damgası.
     *   ARGV[2]: windowMs — zaman penceresi (örn: 60000 = 1 dakika).
     *   ARGV[3]: limit — pencere içinde izin verilen maksimum istek sayısı.
     *
     * Fallback — neden localTokenBucket?
     *   Redis bağlantı hatası: Dağıtık rate limiting çalışmaz.
     *   Fallback olmadan: Tüm istekler reddedilir veya kabul edilir (ikisi de kötü).
     *   localTokenBucket: Tek pod için yerel koruma — Redis gelene kadar geçici çözüm.
     *   Dikkat: Fallback'te N pod varsa her biri bağımsız sayar → toplam limit N×100.
     */
    public boolean isAllowedRedis(String key, int limit, long windowMs) {
        try {
            long now = System.currentTimeMillis();
            Object result = redis.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class),
                    java.util.List.of("rl:" + key), // KEYS[1]
                    String.valueOf(now),             // ARGV[1]: şu anki zaman
                    String.valueOf(windowMs),         // ARGV[2]: pencere süresi
                    String.valueOf(limit)             // ARGV[3]: limit
            );
            return Long.valueOf(1L).equals(result); // Lua'dan 1 gelirse izin verildi
        } catch (Exception e) {
            // Redis bağlantı hatası → yerel fallback (tek pod koruma)
            return localTokenBucket.allowRequest(key);
        }
    }

    /**
     * Token Bucket — Yerel Rate Limiting (Tek Instance).
     *
     * Token Bucket algoritması:
     *   Başlangıç: Kova 100 jeton ile dolu (burst kapasitesi).
     *   Her saniye: 10 jeton eklenir (yenileme hızı).
     *   Her istek: 1 jeton harcar.
     *   Kova dolu: Yeni jeton eklenmez (max 100).
     *   Kova boş: İstek reddedilir.
     *
     * Avantaj: Burst izni.
     *   Uzun süre sessiz kullanıcı: Jeton birikir → ani 100 istek yapabilir.
     *   Sliding window: Burst yakınsa limit aşılmış sayılır.
     *   Token bucket: Birikmiş jeton ile burst kabul edilir.
     *
     * Kullanım: Dev/test, single-instance deployment, veya Redis fallback.
     */
    public boolean isAllowedTokenBucket(String clientId) {
        return localTokenBucket.allowRequest(clientId);
    }

    /**
     * Sliding Window — Yerel Rate Limiting (Kesin Pencere).
     *
     * Sliding Window: Son 60 saniyedeki istek sayısını takip eder.
     *   Token bucket'tan farkı: "Son 60 saniye" her an yeniden hesaplanır.
     *   Fixed window: 00:00-01:00, 01:00-02:00 gibi sabit aralıklar.
     *   Sliding window: Şu andan 60 sn geriye bak → kesin ve adil.
     *
     * Fixed window sorunu:
     *   01:00'da 100 istek + 01:01'de 100 istek: Arka arkaya pencereler dolabilir.
     *   Sliding: 01:00-02:00 penceresi her istek için hesaplanır → her zaman son 60sn.
     */
    public boolean isAllowedSlidingWindow(String clientId) {
        return localSlidingWindow.allowRequest(clientId);
    }

    /**
     * Rate Limiter durumu — monitoring ve debugging için.
     * Mevcut jeton sayısı ve penceredeki istek sayısı izlenebilir.
     */
    public Map<String, Object> getStatus(String clientId) {
        return Map.of(
                "clientId", clientId,
                "tokenBucketAvailable", localTokenBucket.getAvailableTokens(clientId),
                "slidingWindowCount", localSlidingWindow.getCurrentCount(clientId)
        );
    }
}
