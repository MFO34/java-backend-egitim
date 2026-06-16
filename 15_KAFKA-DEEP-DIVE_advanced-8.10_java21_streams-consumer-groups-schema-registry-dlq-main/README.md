# 15 — Kafka Deep Dive

**Difficulty:** Advanced (8/10) · **Java 21** · **Spring Boot 3.2.5**

Apache Kafka ile yüksek throughput event streaming — producer/consumer desenleri, consumer groups, offset yönetimi, DLQ, transactional messaging.

---

## Kafka Mimarisi

```
Producer → [Topic: orders-topic]
              │
              ├── Partition 0 ──→ Consumer Group A (consumer-0)
              ├── Partition 1 ──→ Consumer Group A (consumer-1)
              └── Partition 2 ──→ Consumer Group A (consumer-2)
                                              ↑
                                    Paralel tüketim
                                    (partition sayısı = max paralel consumer)
```

---

## Producer Desenleri

### Acks ve Güvenilirlik
```yaml
acks: all          # tüm ISR ack ister → en yüksek güvence (yavaş)
acks: 1            # leader ack yeterli → orta
acks: 0            # ack bekleme → en hızlı, kayıp riski
enable.idempotence: true  # exactly-once (producer-side, acks=all zorunlu)
```

### Gönderim Modları
```java
// Fire-and-forget
kafkaTemplate.send(topic, key, event);

// Async callback
kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
    if (ex == null) log.info("partition={}, offset={}", ...);
    else sendToDlq(event, ex.getMessage());
});

// Sync (blocking)
SendResult<?, ?> result = kafkaTemplate.send(topic, key, event).get();

// Transactional (atomic multi-topic)
kafkaTemplate.executeInTransaction(ops -> {
    ops.send(ordersTopic, key, orderEvent);
    ops.send(paymentsTopic, key, paymentEvent);
    return true;
});
```

### Key ve Partition Routing
```java
// Aynı key → aynı partition → sıra garantisi
kafkaTemplate.send(topic, customerId, event);  // customerId = key

// Belirli partition
new ProducerRecord<>(topic, partition, key, value);
```

### Headers
```java
ProducerRecord<?, ?> record = new ProducerRecord<>(topic, key, value);
record.headers()
    .add("trace-id", traceId.getBytes())
    .add("source", "order-service".getBytes());
```

---

## Consumer Desenleri

### Manuel Commit (At-Least-Once)
```java
@KafkaListener(topics = "...", containerFactory = "kafkaListenerContainerFactory")
public void process(OrderEvent event, Acknowledgment ack) {
    try {
        handle(event);
        ack.acknowledge();              // başarılı → commit
    } catch (Exception e) {
        ack.nack(Duration.ofSeconds(5)); // 5sn sonra redeliver
    }
}
```

### Batch Consumer
```java
@KafkaListener(topics = "...", containerFactory = "batchKafkaListenerContainerFactory")
public void processBatch(List<ConsumerRecord<String, OrderEvent>> records, Acknowledgment ack) {
    records.forEach(r -> handle(r.value()));
    ack.acknowledge(); // toplu commit
}
```

### Specific Partition
```java
@KafkaListener(topicPartitions = @TopicPartition(topic = "orders", partitions = {"0"}))
public void processPartition0(OrderEvent event) { ... }
```

### Consumer Group — Offset Semantics
```
auto-offset-reset: earliest  → yeni group → partition başından oku
auto-offset-reset: latest    → yeni group → şu anki son offsetten devam
enable-auto-commit: false    → MANUAL_IMMEDIATE ile kontrol sende
```

---

## Consumer Group Load Balancing

```
3 partition, 3 consumer → ideal
3 partition, 2 consumer → 1 consumer 2 partition alır (overloaded)
3 partition, 4 consumer → 1 consumer idle (boş)
Consumer düşerse → rebalance → diğerleri partition'ları alır
```

---

## Dead Letter Queue (DLQ)

```
Producer → [orders-topic]
               ↓
           Consumer (exception)
               ↓ (max retry aşıldı)
           [orders-topic.DLQ]
               ↓
           DLQ Consumer (alarm / replay)
```

```java
// Manuel DLQ yönlendirme
ProducerRecord<?, ?> dlqRecord = new ProducerRecord<>(dlqTopic, key, failedEvent);
dlqRecord.headers().add("dlq-reason", reason.getBytes());
kafkaTemplate.send(dlqRecord);
ack.acknowledge(); // orijinali commit — tekrar deneme yok
```

---

## Topic Konfigürasyonu

```java
TopicBuilder.name("orders-topic")
    .partitions(3)
    .replicas(3)          // prod: min 3 replica
    .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")   // 7 gün
    .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")   // acks=all ile birlikte
    .build();
```

---

## Kafka vs ActiveMQ

| Özellik | Kafka | ActiveMQ |
|---------|-------|----------|
| Model | Append-only log | Queue/Topic |
| Retention | Süre/boyut bazlı | Tüketilince silinir |
| Throughput | Çok yüksek | Orta |
| Replay | Offset ile her zaman | Hayır |
| Use case | Event streaming, CDC | Task queue, RPC |
| Sıra | Partition içinde | FIFO (Priority ile değişir) |

---

## Mülakat Soruları

**Q: Kafka'da sıra (ordering) garantisi nasıl sağlanır?**
A: Partition içinde mesajlar FIFO sırasında tutulur — global sıra garantisi yoktur. Aynı key'li mesajlar aynı partition'a gider (key hash % partition count), dolayısıyla aynı key → aynı consumer → sıralı işlem. Kullanım: Aynı siparişin olaylarını (oluşturuldu, ödendi, kargoya verildi) sıralı işlemek için `orderId` key kullan. Global sıra gerekiyorsa tek partition — throughput düşer, tek consumer zorunlu. Dikkat: Rebalance veya partition ekleme sıra garantisini bozabilir (yeni partition'a key map'i değişir).

**Q: `acks=0`, `acks=1`, `acks=all` farkı nedir?**
A: `acks=0` (fire-and-forget): Broker ack beklemez — en hızlı, veri kaybı riski var (broker çökerse). `acks=1` (leader ack): Sadece partition leader yazdı ise ack — lider çökerse follower'a sync olmayan mesaj kaybolur. `acks=all` (ISR ack): Tüm In-Sync Replica'lar yazdı ise ack — en güvenli, en yavaş. `min.insync.replicas=2` ile: En az 2 replica sync'de olmadan mesaj kabul etme. Seçim: Analytics, log → `acks=1`; ödeme, kritik event → `acks=all`. `acks=all` + `enable.idempotence=true` + transactional → exactly-once.

**Q: `enable.idempotence=true` ne işe yarar? Exactly-once nasıl sağlanır?**
A: Idempotent producer: Her mesaja sequence number ekler, broker duplicate'leri reddeder. Retry durumunda mesaj iki kez yazılmaz. Gereksinim: `acks=all`, `max.in.flight.requests.per.connection=5`. Exactly-once producer semantics: producer tarafında duplicate yok. Consumer tarafında exactly-once: Kafka Transactions (`@Transactional` + transactional producer + `isolation.level=read_committed`). Spring Kafka: `@KafkaListener` + `KafkaTransactionManager` + `ChainedKafkaTransactionManager` ile DB ve Kafka aynı anda commit. Pratik: Tam exactly-once karmaşık; çoğu sistem at-least-once + idempotent consumer ile yetinir.

**Q: Consumer group rebalance ne zaman olur? Nasıl minimize edilir?**
A: Rebalance tetikleyicileri: Consumer join/leave, `session.timeout.ms` (heartbeat yok), `max.poll.interval.ms` (işleme çok uzun sürdü), partition sayısı değişti. Stop-the-world: Tüm consumer'lar durur, partition'lar yeniden atanır — bu sürede mesaj işlenmez. Minimize etme: `session.timeout.ms` ve `heartbeat.interval.ms` iyileştir (10s/3s). `max.poll.interval.ms`'i işleme süresine göre artır. Incremental Cooperative Rebalancing (`partition.assignment.strategy=CooperativeStickyAssignor`): Tüm consumer'lar durmadan, sadece taşınan partition'lar duraksar. Kubernetes scale-down: Consumer'ı düzgünce kapat (`close()`) → partition'lar hemen başkasına devredilir.

**Q: `enable-auto-commit: false` neden kullanılır? At-least-once vs At-most-once?**
A: Auto-commit (true): Kafka her `auto.commit.interval.ms` (default 5s) offset commit eder — mesaj henüz işlenmeden commit olabilir, crash durumunda kayıp (at-most-once). Manual commit (false): `@KafkaListener` metodu başarıyla döndüğünde offset commit et — crash durumunda tekrar işlenir (at-least-once). `Acknowledgment.acknowledge()` ile elle kontrol. Seçim: Her mesajın işlenmesi kritikse → `enable-auto-commit: false` + manual ack. Hız önemliyse, kayıp kabul edilebilirse → auto-commit. At-exactly-once: Kafka Transactions ile ama çok daha karmaşık.

**Q: Partition sayısını nasıl belirlersin?**
A: Formül: `partition = max(desired_throughput / throughput_per_partition)`. Her partition bir consumer'a atanır — paralel tüketim. Daha fazla partition = daha yüksek throughput ama: Daha fazla open file handle (broker), leader election süresi uzar, rebalance süresi uzar. Önerilen: Topic başına 3-10 partition'dan başla, throughput geldikçe artır. Partition azaltılamaz (sadece artırılır) — başlangıçta fazla vermek sorun olmaz. Replication factor: 3 önerilir (1 leader + 2 follower), `min.insync.replicas=2`.

**Q: EmbeddedKafka ile entegrasyon testi nasıl yazılır?**
A: `@EmbeddedKafka(partitions = 1, topics = {"orders"})` — JVM içinde gerçek Kafka broker başlatır, Zookeeper gerektirmez. `spring-kafka-test` bağımlılığı ekle. Test yapısı: `@SpringBootTest` + `@EmbeddedKafka`. Producer'ı çağır, `Consumer.poll()` ile mesajı al veya `CountDownLatch` ile listener'ın işlediğini bekle. `@DirtiesContext`: Her test sonrası context temizle — topic state sıfırlansın. Avantaj: Gerçek Kafka davranışı (serialization, consumer group, offset), Docker gerektirmez. Testcontainers alternatifi: Gerçek Docker Kafka — daha ağır ama production yakın.

---

## Çalıştırma

```bash
# Kafka + Zookeeper (docker-compose)
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  confluentinc/cp-kafka:latest

# Uygulama
mvn spring-boot:run

# Test (EmbeddedKafka — broker gerekmez)
mvn test
```
