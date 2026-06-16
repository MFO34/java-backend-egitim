package com.autohub.config;

// Kafka importları
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Apache Kafka Konfigürasyonu
 *
 * Kafka topic'leri, producer ve consumer ayarlarını yönetir.
 * Event-driven mimari için gerekli tüm Kafka bean'larını tanımlar.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Kafka Topic Adları
    private static final String VEHICLE_CREATED_TOPIC = "vehicle-created";
    private static final String VEHICLE_UPDATED_TOPIC = "vehicle-updated";
    private static final String VEHICLE_DELETED_TOPIC = "vehicle-deleted";
    private static final String VEHICLE_STATUS_CHANGED_TOPIC = "vehicle-status-changed";
    private static final String TRANSACTION_CREATED_TOPIC = "transaction-created";

    /**
     * Araç oluşturma topic'i
     * partitions=3: 3 partition - paralel işleme kapasitesi
     * replicationFactor=1: 1 replika (geliştirme için yeterli, üretimde 3 olmalı)
     */
    @Bean
    public NewTopic vehicleCreatedTopic() {
        return new NewTopic(VEHICLE_CREATED_TOPIC, 3, (short) 1);
    }

    @Bean
    public NewTopic vehicleUpdatedTopic() {
        return new NewTopic(VEHICLE_UPDATED_TOPIC, 3, (short) 1);
    }

    @Bean
    public NewTopic vehicleDeletedTopic() {
        return new NewTopic(VEHICLE_DELETED_TOPIC, 3, (short) 1);
    }

    @Bean
    public NewTopic vehicleStatusChangedTopic() {
        return new NewTopic(VEHICLE_STATUS_CHANGED_TOPIC, 3, (short) 1);
    }

    @Bean
    public NewTopic transactionCreatedTopic() {
        return new NewTopic(TRANSACTION_CREATED_TOPIC, 3, (short) 1);
    }

    /**
     * Kafka Producer fabrikası
     * JSON serileştirme kullanır - objeler JSON formatında gönderilir
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Anahtar serileştirici: String
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Değer serileştirici: JSON
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Tüm broker'lar mesajı onaylayana kadar bekle (güvenilirlik için)
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // Hata durumunda 3 kez tekrar dene
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        // Çoğaltma hatasını önlemek için idempotent producer
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka Template - mesaj göndermek için kullanılır
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka Consumer fabrikası
     * JSON deserileştirme kullanır - JSON mesajlar Java objelerine dönüştürülür
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "autohub-consumer-group");
        // Anahtar deserileştirici
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Değer deserileştirici: JSON
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Güvenilen paketler - deserileştirme güvenliği için
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.autohub.*");
        // Consumer yeniden başladığında en baştan oku
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Manuel offset commit - işleme tamamlandıktan sonra commit et
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Kafka Listener Container Factory
     * @KafkaListener anotasyonları için gerekli
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        // Paralel consumer thread sayısı
        factory.setConcurrency(3);
        // Manuel acknowledgment - işleme tamamlandıktan sonra commit
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    /**
     * Kafka Admin Client - topic yönetimi için
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(config);
    }
}
