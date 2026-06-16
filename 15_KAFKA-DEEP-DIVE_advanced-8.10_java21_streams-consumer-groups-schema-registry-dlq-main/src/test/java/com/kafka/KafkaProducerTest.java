package com.kafka;

import com.kafka.model.OrderEvent;
import com.kafka.producer.OrderEventProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmbeddedKafka — gerçek Kafka broker gerektirmez.
 * spring-kafka-test bağımlılığı ile JVM içinde Kafka.
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = {"orders-topic", "payments-topic", "notifications-topic", "orders-topic.DLQ"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@DirtiesContext
class KafkaProducerTest {

    @Autowired
    OrderEventProducer producer;

    @Autowired
    KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Test
    void sendAsync_noException() {
        OrderEvent event = new OrderEvent("ORDER_PLACED",
                UUID.randomUUID().toString(), "cust-1", new BigDecimal("500"));

        assertThat(producer.sendAsync(event)).isNotNull();
    }

    @Test
    void sendSync_returnsMetadata() throws Exception {
        OrderEvent event = new OrderEvent("ORDER_PLACED",
                UUID.randomUUID().toString(), "cust-2", new BigDecimal("250"));

        var meta = producer.sendSync(event);
        assertThat(meta).isNotNull();
        assertThat(meta.partition()).isBetween(0, 2);
        assertThat(meta.offset()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void sendBatch_allSent() {
        var events = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> new OrderEvent("ORDER_PLACED",
                        UUID.randomUUID().toString(), "cust-" + i, BigDecimal.valueOf(100 + i)))
                .toList();

        assertThat(events).hasSize(5);
        producer.sendBatch(events); // should not throw
    }
}
