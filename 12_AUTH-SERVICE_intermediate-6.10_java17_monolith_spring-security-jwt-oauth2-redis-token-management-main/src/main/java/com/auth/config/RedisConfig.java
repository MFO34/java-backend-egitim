package com.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * REDİS YAPILANDIRMASI
 * ====================
 * Redis bağlantısını ve StringRedisTemplate bean'ini yapılandırır.
 *
 * Redis nedir?
 *   In-memory (bellek içi) key-value veri deposu.
 *   Disk yerine RAM'de çalışır → çok hızlı okuma/yazma.
 *   TTL (Time-To-Live) desteği: "5 dakika sonra bu key'i sil"
 *
 * Biz neden Redis kullanıyoruz?
 *   JWT Blacklist: Logout olan kullanıcının token'ını geçersiz kılmak için.
 *   Token DB'de değil, Redis'te saklanır:
 *     key:   "blacklist:jti:<uuid>"
 *     value: "revoked"
 *     TTL:   token'ın kalan süresi
 *
 * StringRedisTemplate vs RedisTemplate:
 *   RedisTemplate<Object, Object>   → her şeyi Java serialize eder (JdkSerializationRedisSerializer)
 *   StringRedisTemplate             → hem key hem value'yu String olarak saklar
 *   Biz String kullandığımız için StringRedisTemplate yeterli ve daha verimli.
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate Bean
     * ========================
     * Spring Boot otomatik StringRedisTemplate oluşturur ama
     * burada serializer'ları açıkça ayarlıyoruz — best practice.
     *
     * RedisConnectionFactory: application.yml'deki spring.data.redis.*
     * ayarlarından Spring Boot tarafından otomatik oluşturulur.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new StringRedisTemplate();

        // Bağlantı factory'sini set et
        template.setConnectionFactory(connectionFactory);

        // Key serializer: String → byte[] (UTF-8)
        template.setKeySerializer(new StringRedisSerializer());

        // Value serializer: String → byte[]
        template.setValueSerializer(new StringRedisSerializer());

        // Hash key/value serializer (HSET komutları için)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        // Sonra afterPropertiesSet çağırılmazsa serializer'lar uygulanmaz
        template.afterPropertiesSet();

        return template;
    }
}
