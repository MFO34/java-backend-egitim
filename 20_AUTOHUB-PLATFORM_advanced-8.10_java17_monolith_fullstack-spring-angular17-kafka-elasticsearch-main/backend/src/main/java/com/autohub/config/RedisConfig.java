package com.autohub.config;

// Redis ve Spring importları
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Önbellek Konfigürasyonu
 *
 * Redis, veritabanı sorgularını önbelleğe alarak uygulama performansını artırır.
 * Sık okunan veriler (araç listesi, marka listesi) Redis'te tutulur.
 *
 * OOP Prensibi - KAPSÜLLEME:
 * Redis konfigürasyon detayları bu sınıfta gizlenir.
 */
@Configuration
public class RedisConfig implements CachingConfigurer {

    /**
     * Redis template - manuel cache işlemleri için
     * String anahtar, JSON değer serileştirme kullanır
     *
     * @param connectionFactory Redis bağlantı fabrikası (Spring tarafından oluşturulur)
     * @return RedisTemplate instance'ı
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // Bağlantı fabrikasını set et
        template.setConnectionFactory(connectionFactory);

        // Anahtar serileştirici: String - okunabilir anahtarlar
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Değer serileştirici: JSON - objeleri JSON formatında sakla
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Template ayarlarını uygula
        template.afterPropertiesSet();

        return template;
    }

    /**
     * Cache Manager - @Cacheable anotasyonları için
     * Farklı cache'ler için farklı TTL (Time-To-Live) değerleri ayarlanır
     *
     * @param connectionFactory Redis bağlantı fabrikası
     * @return RedisCacheManager instance'ı
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        return null; // Spring Boot auto-configuration kullanılıyor
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Varsayılan cache konfigürasyonu
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            // Değerleri JSON olarak sakla
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            // Anahtarları String olarak sakla
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            // Varsayılan TTL: 10 dakika
            .entryTtl(Duration.ofMinutes(10))
            // Null değerleri önbellekle - NullPointerException önleme
            .disableCachingNullValues();

        // Cache adlarına özel TTL değerleri
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Araç önbelleği - 15 dakika (sık değişmez)
        cacheConfigurations.put("vehicle",
            defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Araç listesi önbelleği - 5 dakika (daha sık güncellenir)
        cacheConfigurations.put("vehicles",
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Marka önbelleği - 1 saat (nadiren değişir)
        cacheConfigurations.put("brand",
            defaultConfig.entryTtl(Duration.ofHours(1)));

        // Marka listesi önbelleği - 30 dakika
        cacheConfigurations.put("brands",
            defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Araç istatistikleri - 5 dakika
        cacheConfigurations.put("vehicleStats",
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Cache manager oluştur
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
