package com.sysdesign.ratelimiter.algorithm;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Log Rate Limiter
 *
 * Her isteğin timestamp'ini saklar.
 * İstek gelince window dışı timestamp'leri temizle,
 * kalanlar limit altındaysa geç.
 *
 * Avantaj: En doğru — window sınırındaki burst sorunu yok
 * Dezavantaj: Memory kullanımı yüksek (her request loglanır)
 *
 * Distributed: Redis ZSET (score = timestamp)
 *   ZREMRANGEBYSCORE key 0 (now - windowMs)   → eski kayıtları sil
 *   ZCARD key                                  → mevcut sayı
 *   ZADD key now now                           → yeni kayıt ekle
 *   EXPIRE key windowSeconds
 *
 * Hepsi Lua script'te atomic çalışır
 */
public class SlidingWindowRateLimiter {

    private final int maxRequests;
    private final long windowMs;

    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public synchronized boolean allowRequest(String key) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - windowMs;

        Deque<Long> log = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Window dışı girişleri temizle
        while (!log.isEmpty() && log.peekFirst() < windowStart) {
            log.pollFirst();
        }

        if (log.size() < maxRequests) {
            log.addLast(now);
            return true;
        }
        return false;
    }

    public int getCurrentCount(String key) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - windowMs;
        Deque<Long> log = requestLog.getOrDefault(key, new ArrayDeque<>());
        return (int) log.stream().filter(t -> t >= windowStart).count();
    }
}
