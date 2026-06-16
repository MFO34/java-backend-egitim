package com.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topic konfigürasyonu:
 * - partition sayısı → paralel consumer sayısını belirler
 * - replication factor → dayanıklılık (prod'da 3)
 * - retention → mesaj tutma süresi
 * - compaction → key bazlı son değer tutma (state store)
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.topics.orders}")
    private String ordersTopic;

    @Value("${app.topics.payments}")
    private String paymentsTopic;

    @Value("${app.topics.notifications}")
    private String notificationsTopic;

    @Value("${app.topics.dlq}")
    private String dlqTopic;

    // 3 partition → 3 consumer thread ile tam paralel
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(ordersTopic)
                .partitions(3)
                .replicas(1)  // prod'da 3
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 gün
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name(paymentsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(notificationsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // Compacted topic — key başına son event (audit log, state store)
    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 gün
                .build();
    }
}
