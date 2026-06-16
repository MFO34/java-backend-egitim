# 24 — System Design

**Difficulty:** Senior (9/10) · **Architecture** · **Distributed Systems**

Mülakatlarda en çok sorulan system design soruları: URL Shortener, Rate Limiter, Consistent Hashing, Bloom Filter ve dağıtık sistemler kavramları.

---

## 1. URL Shortener

### Gereksinimler
```
Fonksiyonel:
  - URL kısaltma (→ 7 karakter kod)
  - Yönlendirme (302 redirect)
  - Analytics (click count)
  - TTL desteği

Non-fonksiyonel:
  - 100M write/gün, 1B read/gün (10:1 okuma ağırlıklı)
  - Düşük latency (<10ms read)
  - 99.99% availability
  - 10 yıllık veri: ~365B URL
```

### Kapasite Hesabı
```
Write: 100M/gün = ~1.200 write/sn
Read:  1B/gün   = ~12.000 read/sn
Storage (5 yıl): 100M * 365 * 5 * ~500 bytes ≈ 100TB
```

### Kod Üretimi — Base62
```
Base62 alfabe: 0-9, A-Z, a-z (62 karakter)
7 karakter: 62^7 = 3.5 trilyon URL
Yaklaşım: Sayaç (distributed counter) → Base62 encode

Alternatifler:
  - MD5(url) → ilk 7 char: çakışma riski
  - UUID → tahmin edilemez ama uzun
  - Snowflake ID → distributed, sıralı, 64bit → Base62 ✓
```

### Mimari
```
Client → Load Balancer → App Servers (stateless)
                              │
                    ┌─────────┼─────────┐
                 Redis      Kafka     PostgreSQL
                (cache)   (write     (master +
                          buffer)     replicas)

Write path: App → Kafka → Async Consumer → DB
Read path:  App → Redis cache → (miss) → DB → cache'le
```

### 301 vs 302 Redirect
```
301 Permanent → browser cache'ler, sunucuya tekrar gelmez (analytics kayıp)
302 Temporary → her seferinde sunucuya gelir (analytics doğru) ✓
```

---

## 2. Rate Limiter

### Algoritma Karşılaştırması
```
┌─────────────────┬──────────┬─────────┬──────────────────────────────┐
│ Algoritma       │ Burst    │ Memory  │ Not                          │
├─────────────────┼──────────┼─────────┼──────────────────────────────┤
│ Token Bucket    │ Evet     │ O(1)    │ AWS API GW, Stripe ✓         │
│ Leaky Bucket    │ Hayır    │ O(1)    │ Sabit çıkış, smooth traffic  │
│ Fixed Window    │ Sınırda  │ O(1)    │ Basit, window burst problemi │
│ Sliding Window  │ Hayır    │ O(n)    │ En doğru, pahalı             │
│ Sliding Counter │ Kısmi    │ O(1)    │ Redis ZSET ile verimli ✓     │
└─────────────────┴──────────┴─────────┴──────────────────────────────┘
```

### Fixed Window Problemi
```
Limit: 100 req/dk
00:00:50 → 100 istek → doldu
00:01:10 → 100 istek → yeni window → geçti!
→ 20 saniyede 200 istek — burst problem
```

### Redis Lua Script (Distributed, Atomic)
```lua
local key = KEYS[1]
local now = ARGV[1]
local window = ARGV[2]   -- ms
local limit = ARGV[3]

redis.call('ZREMRANGEBYSCORE', key, 0, now - window)  -- eski sil
local count = redis.call('ZCARD', key)               -- say

if count < limit then
    redis.call('ZADD', key, now, now)                -- ekle
    redis.call('PEXPIRE', key, window)               -- TTL
    return 1  -- allowed
else
    return 0  -- rejected
end
```

### Rate Limit Response Headers
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1716000060
Retry-After: 30          (429 Too Many Requests ile)
```

---

## 3. Consistent Hashing

### Problem
```
Modulo hash: hash(key) % N
N = 3 → N = 4: neredeyse tüm key'ler yeni node'a taşınır (%75)
→ Cache stampede, DB flood
```

### Consistent Hashing
```
Hash ring (0..2^32)

    0
   /│\
  / │  \
n3  │   n1
  \ │  /
   \│/
   n2

Key'in hash'i ring'e atılır, saat yönünde ilk node sorumlulu.

Node ekleme: sadece yeni node ile komşusu arasındaki key'ler taşınır
N = 100, 1 node çıkarma → sadece ~%1 key etkilenir
```

### Virtual Nodes (vnodes)
```java
// Her fiziksel node → K sanal node
ring.addNode("node1");  // ring'e node1#0, node1#1, ..., node1#99 eklenir

// Avantaj: dengesiz dağılım önlenir
// k = 100-200 vnode önerilir (Cassandra: 256 default)
```

### Kullanım
```
Cassandra   → partition key routing
Redis Cluster → slot routing (16384 slot, CRC16 hash)
CDN          → edge server seçimi
Load Balancer → session affinity (sticky session)
```

---

## 4. Bloom Filter

### Ne Zaman Kullanılır?
```
"Bu eleman kesinlikle yok mu?" sorusuna hızlı yanıt.

Yanlış negatif → imkânsız (var diyip yok deme)
Yanlış pozitif → mümkün (yok diyip var deme) ← %1-2 kabul edilebilir
```

### Kullanım Alanları
```
URL Shortener  → kod üretilmiş mi? (DB'ye gitmeden)
Cache          → cache miss mi? (DB'ye gitme gerek yok)
Cassandra      → sstable'da row var mı? (disk okuma önle)
Chrome         → malware URL kontrolü (yerel filtre)
Bitcoin        → UTXO set membership
HBase          → block cache içinde mi?
```

### Hesaplama
```
n = 1M eleman, p = 0.01 (1% false positive)
m = -n * ln(p) / (ln2)^2 ≈ 9.6M bit ≈ 1.2 MB
k = (m/n) * ln2 ≈ 7 hash fonksiyonu
```

---

## Yaygın System Design Soruları

### Tasarım Adımları (RESHADED Framework)
```
1. Requirements (functional + non-functional)
2. Estimations (QPS, storage, bandwidth)
3. System interface (API definition)
4. High-level design
5. API design detail
6. Database design
7. Elaborate (deep dive — bottleneck)
```

### CAP Teoremi
```
Consistency  → her node aynı veriyi döner
Availability → her istek yanıt alır
Partition Tolerance → ağ bölünmesi tolere edilir

→ P'den vazgeçilemez (ağ hataları gerçek)
→ CP: MongoDB, HBase, Zookeeper (bank transaction)
→ AP: Cassandra, CouchDB, DynamoDB (social media)
```

### ACID vs BASE
```
ACID: Atomicity, Consistency, Isolation, Durability → SQL, strong consistency
BASE: Basically Available, Soft state, Eventually consistent → NoSQL
```

### Temel Sayılar (2^N)
```
1 Byte   = 8 bit
1 KB     = 10^3 bytes
1 MB     = 10^6 bytes
1 GB     = 10^9 bytes
1 TB     = 10^12 bytes

Latency:
  L1 cache     0.5ns
  RAM          100ns
  SSD read     100μs
  HDD seek     10ms
  Network RTT  150ms (cross-continental)
```

---

## REST Endpoints

| Method | URL | Açıklama |
|--------|-----|----------|
| POST | `/api/url/shorten?url=https://...` | URL kısalt |
| GET | `/api/url/{code}` | 302 Redirect |
| GET | `/api/url/{code}/stats` | Click analytics |
| GET | `/api/rate-limit/test` | Token bucket test |
| GET | `/api/rate-limit/sliding-window` | Sliding window test |
| GET | `/api/rate-limit/status/{clientId}` | Durum |

---

## Çalıştırma

```bash
# Uygulama (Redis opsiyonel)
mvn spring-boot:run

# Testler (Redis gerektirmez)
mvn test -Dtest=SystemDesignTest

# URL kısalt
curl -X POST "http://localhost:8080/api/url/shorten?url=https://github.com"

# Rate limit test (5 kez çalıştır — 6'da 429)
for i in {1..6}; do curl -w "\n" http://localhost:8080/api/rate-limit/test; done
```

---

## Mülakat Soruları

**Q: URL kısaltma sistemini nasıl tasarlarsın? (System Design)**
A: Gereksinimler: 100M write/gün, 1B read/gün, 99.99% availability, <10ms read. Kapasite: 1.200 write/sn, 12.000 read/sn. Kod üretimi: Counter (distributed, Snowflake ID) → Base62 encode → 7 karakter (62^7 = 3.5T unique URL). Mimari: Load Balancer → Stateless App Server → Redis (hot URL cache) → PostgreSQL (master + read replica). Write path: App → Kafka → async consumer → DB. Read path: Redis hit (cache) → miss → DB → Redis write. 301 vs 302: 302 tercih edilir — analytics için her seferinde sunucu görür.

**Q: Rate Limiter algoritmalarını karşılaştır.**
A: Token Bucket: Kova belirli kapasitede token tutar, periyodik olarak dolar. Burst toleransı var (kovanın doluluğu kadar). AWS, Stripe kullanır. Leaky Bucket: Sabit hızda çıkış, burst yok — smooth traffic. Fixed Window: Her dakikada N istek. Sorun: Dakika geçişinde 2N istek mümkün (pencere sınırında burst). Sliding Window Log: Her istek timestamp ile ZSET'te. En doğru ama O(n) bellek. Sliding Window Counter: Fixed Window'u örtüşmeyle tahmin — O(1) bellek, %99 doğruluk. Seçim: Burst gerekiyorsa Token Bucket, düzgün trafik Leaky Bucket, doğruluk Sliding Window.

**Q: Consistent Hashing neden gereklidir?**
A: Modulo hashing: `hash(key) % N`. N=3 → N=4 olunca %75 key yeni node'a gider — tüm cache geçersiz, DB flood. Consistent Hashing: Hash ring (0..2^32). Key'in hash'i ring'e atılır, saat yönünde ilk node sorumlu. Node ekleme/çıkarmada sadece komşu key'ler etkilenir (1/N). Virtual nodes: Her fiziksel node K sanal node → daha dengeli dağılım (Cassandra: 256 vnode). Kullanım: Redis Cluster (16384 slot, CRC16), Cassandra partition routing, CDN edge seçimi.

**Q: Bloom Filter ne zaman kullanılır? False positive neden kabul edilebilir?**
A: Bloom Filter: "Bu eleman kesinlikle yok mu?" sorusuna hızlı yanıt. False negative imkânsız (var diyip yok deme). False positive mümkün (%1-2 kabul edilebilir). Kullanım: URL Shortener — kod üretilmiş mi? (DB'ye gitmeden). Cache — DB'de yok ise cache miss bile yaratma. Cassandra sstable'da satır var mı? Chrome malware URL kontrolü. Hesap: 1M eleman, %1 false positive → 9.6 Mbit (1.2 MB), 7 hash fonksiyonu. O(k) time ve space — k hash sayısı.

**Q: CAP teoremi nedir ve gerçekte nasıl uygulanır?**
A: Consistency: tüm node'lar aynı veriyi döner. Availability: her istek yanıt alır. Partition Tolerance: ağ bölünmesine rağmen çalışır. P'den vazgeçilemez — ağ hataları gerçek. CA (P olmadan): tek node sistemi, dağıtık değil. CP: Partition durumunda availability feda edilir — MongoDB, HBase, Zookeeper. Bank transaction için. AP: Partition durumunda consistency feda edilir — Cassandra, DynamoDB, CouchDB. Social media için. Pratikte: "CP vs AP" — hata durumunda ne yapalım sorusu.

**Q: RESHADED framework nedir? System design mülakatında nasıl kullanılır?**
A: R-Requirements: Fonksiyonel (ne yapacak?) + Non-fonksiyonel (QPS, latency, availability). E-Estimations: QPS hesabı, storage, bandwidth. S-System interface: API endpoint'leri tanımla (REST/gRPC). H-High-level design: Mimari diyagram (LB, App, Cache, DB, Queue). A-API design detail: Request/Response şeması. D-Database design: Schema, indexler, sharding. E-Elaborate: Bottleneck bul, deep dive (caching, sharding, replication). 45 dakikalık mülakatta adım adım ilerle, her adımda interviewer ile konuş.

**Q: Latency numaralarını biliyor olman neden önemlidir?**
A: System design kararları latency bağımlı. L1 cache (0.5ns) vs RAM (100ns) vs SSD (100µs) vs HDD (10ms) vs Network (150ms). Örnek: "Redis mi, DB mi?" — Redis RAM'de (~0.1ms), DB SSD'de (~1-10ms). 10:1 fark var. "Her istek için DB query mi, cache mi?" — 100 eş zamanlı kullanıcı × 10ms DB = 1 saniye, × 0.1ms Redis = 10ms. Latency bilinmeden doğru karar verilemez. Interviewer'lar bu sayıları biliyor olmanı bekler.
