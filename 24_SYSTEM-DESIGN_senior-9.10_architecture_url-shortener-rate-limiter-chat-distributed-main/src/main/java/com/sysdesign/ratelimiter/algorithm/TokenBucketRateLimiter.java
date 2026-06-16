package com.sysdesign.ratelimiter.algorithm;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket Rate Limiter
 *
 * Algoritma:
 *   - Kova kapasitesi = maxTokens
 *   - Her saniye refillRate kadar token eklenir
 *   - İstek gelince 1 token tüketilir
 *   - Kova doluyken yeni token eklenmez
 *   - Token yoksa istek reddedilir (429)
 *
 * Avantaj: Burst trafiğe izin verir (kova doluysa)
 * Dezavantaj: Distributed ortamda koordinasyon gerekir (Redis EVAL/Lua)
 *
 * Karşılaştırma:
 * ┌─────────────────┬──────────────┬────────────────────────────┐
 * │ Algoritma       │ Burst        │ Not                        │
 * ├─────────────────┼──────────────┼────────────────────────────┤
 * │ Token Bucket    │ Evet         │ En yaygın, AWS/Stripe      │
 * │ Leaky Bucket    │ Hayır        │ Sabit çıkış hızı, smooth  │
 * │ Fixed Window    │ Evet (sınırda)│ Basit, window sınırı burstu│
 * │ Sliding Window  │ Hayır        │ En doğru, pahalı           │
 * │ Sliding Counter │ Kısmi        │ Redis ZRANGEBYSCORE        │
 * └─────────────────┴──────────────┴────────────────────────────┘
 */
public class TokenBucketRateLimiter {

    private final long maxTokens;
    private final long refillRatePerSecond;

    // key = userId veya IP
    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long maxTokens, long refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    public boolean allowRequest(String key) {
        BucketState state = buckets.computeIfAbsent(key, k -> new BucketState(maxTokens));
        return state.consume();
    }

    public long getAvailableTokens(String key) {
        BucketState state = buckets.get(key);
        return state != null ? state.refillAndGet() : maxTokens;
    }

    private class BucketState {
        private volatile long tokens;
        private volatile long lastRefillTime;

        BucketState(long initialTokens) {
            this.tokens = initialTokens;
            this.lastRefillTime = Instant.now().getEpochSecond();
        }

        synchronized boolean consume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        synchronized long refillAndGet() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = Instant.now().getEpochSecond();
            long elapsed = now - lastRefillTime;
            if (elapsed > 0) {
                long toAdd = elapsed * refillRatePerSecond;
                tokens = Math.min(maxTokens, tokens + toAdd);
                lastRefillTime = now;
            }
        }
    }
}
