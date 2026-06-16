# 🚗 AutoHub - Otomotiv Yönetim Sistemi

AutoHub, araç kiralama ve satış işlemlerini yönetmek için geliştirilmiş kapsamlı bir **fullstack** web uygulamasıdır.

---

## 📋 İçindekiler

- [Teknoloji Yığını](#-teknoloji-yığını)
- [Proje Mimarisi](#-proje-mimarisi)
- [Özellikler](#-özellikler)
- [Kurulum](#-kurulum)
- [Docker ile Çalıştırma](#-docker-ile-çalıştırma)
- [Kubernetes ile Dağıtım](#-kubernetes-ile-dağıtım)
- [API Dokümantasyonu](#-api-dokümantasyonu)
- [Ortam Değişkenleri](#-ortam-değişkenleri)

---

## 🛠 Teknoloji Yığını

### Backend
| Teknoloji | Sürüm | Kullanım |
|-----------|-------|---------|
| Java | 17 | Programlama dili |
| Spring Boot | 3.2 | Uygulama çatısı |
| Spring Security + JWT | - | Kimlik doğrulama |
| Spring Data JPA | - | ORM katmanı |
| PostgreSQL | 15 | Ana veritabanı |
| Redis | 7 | Önbellekleme |
| Apache Kafka | 3.5 | Mesajlaşma platformu |
| Elasticsearch | 8.11 | Arama motoru |
| Spring Cloud Gateway | - | API Gateway |
| Consul | 1.16 | Servis keşfi |
| ActiveMQ Artemis | - | Mesaj kuyruğu |
| Resilience4J | 2.1 | Devre kesici |
| Zipkin | 3 | Dağıtık izleme |
| Prometheus + Grafana | - | İzleme/Metrik |
| Maven | 3.9 | Build aracı |

### Frontend
| Teknoloji | Sürüm | Kullanım |
|-----------|-------|---------|
| Angular | 17 | Frontend çatısı |
| TypeScript | 5.2 | Programlama dili |
| Angular Material | 17 | UI bileşen kütüphanesi |
| RxJS | 7.8 | Reaktif programlama |
| SCSS | - | Stil dili |

### DevOps
| Teknoloji | Kullanım |
|-----------|---------|
| Docker | Konteynerizasyon |
| Docker Compose | Çoklu konteyner yönetimi |
| Kubernetes | Container orkestrasyon |
| Nginx | Frontend web sunucusu |

---

## 🏗 Proje Mimarisi

```
autohub/
├── backend/                    # Spring Boot Backend
│   ├── src/main/java/com/autohub/
│   │   ├── config/            # Konfigürasyon sınıfları (Redis, Kafka, JPA)
│   │   ├── controller/        # REST API Controller'ları
│   │   ├── dto/               # Data Transfer Objects
│   │   │   ├── request/       # İstek DTO'ları
│   │   │   └── response/      # Yanıt DTO'ları
│   │   ├── entity/            # JPA Entity sınıfları
│   │   ├── exception/         # Özel exception sınıfları
│   │   ├── kafka/             # Kafka producer/consumer
│   │   │   ├── producer/
│   │   │   └── consumer/
│   │   ├── repository/        # Spring Data JPA repository'leri
│   │   ├── security/          # JWT ve Spring Security
│   │   └── service/           # İş mantığı servisleri
│   │       └── impl/          # Servis implementasyonları
│   ├── src/main/resources/
│   │   ├── application.yml    # Uygulama konfigürasyonu
│   │   └── db/migration/      # Flyway SQL migration'ları
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                   # Angular Frontend
│   ├── src/app/
│   │   ├── components/        # Paylaşılan bileşenler
│   │   ├── guards/            # Route guard'ları (AuthGuard, RoleGuard)
│   │   ├── interceptors/      # HTTP interceptor'ları (JWT)
│   │   ├── models/            # TypeScript interface/enum tanımları
│   │   ├── modules/           # Lazy-loaded özellik modülleri
│   │   │   ├── auth/          # Login, Register
│   │   │   ├── dashboard/     # Ana sayfa istatistikleri
│   │   │   ├── vehicle/       # Araç yönetimi
│   │   │   ├── brand/         # Marka yönetimi
│   │   │   ├── customer/      # Müşteri yönetimi
│   │   │   └── transaction/   # İşlem yönetimi
│   │   └── services/          # API servis sınıfları
│   ├── Dockerfile
│   └── nginx.conf
│
├── k8s/                       # Kubernetes manifest dosyaları
├── monitoring/                # Prometheus konfigürasyonu
├── docker-compose.yml         # Geliştirme ortamı
└── README.md
```

### Katmanlı Mimari (Clean Architecture)

```
┌─────────────────────────────────────┐
│         Controller Katmanı          │  ← HTTP isteklerini alır, yanıt döner
│  (REST API Endpoint'leri)           │
├─────────────────────────────────────┤
│          Service Katmanı            │  ← İş mantığı, doğrulama
│  (İş Kuralları, Cache, Kafka)       │
├─────────────────────────────────────┤
│        Repository Katmanı           │  ← Veritabanı işlemleri (JPA)
│  (Veri Erişim Katmanı)              │
├─────────────────────────────────────┤
│      Veritabanı / Servisler         │  ← PostgreSQL, Redis, Elasticsearch
└─────────────────────────────────────┘
```

---

## ✨ Özellikler

### Araç Yönetimi
- ✅ Araç ekleme, güncelleme, silme (soft delete)
- ✅ Plaka, VIN numarası ile arama
- ✅ Marka, model, yıl, fiyat bazlı filtreleme
- ✅ Elasticsearch ile full-text arama
- ✅ Araç durumu yönetimi (Müsait, Kiralandı, Satıldı, Bakımda)
- ✅ Araç fotoğrafları ve özellikler

### Marka & Model Yönetimi
- ✅ Araç markası ekleme ve yönetimi
- ✅ Marka-model hiyerarşisi
- ✅ Araç tipi, yakıt tipi, vites tipi yönetimi

### Müşteri Yönetimi
- ✅ Bireysel ve kurumsal müşteri kaydı
- ✅ TC Kimlik numarası doğrulaması
- ✅ Ehliyet geçerlilik kontrolü
- ✅ Müşteri geçmişi

### Kiralama & Satış İşlemleri
- ✅ Kiralama başlangıç/bitiş tarihleri
- ✅ Gecikmiş kiralama tespiti
- ✅ Otomatik KDV hesaplaması
- ✅ İndirim ve ek ücret yönetimi
- ✅ İşlem numarası otomatik üretimi

### Güvenlik
- ✅ JWT tabanlı kimlik doğrulama
- ✅ Role-based access control (RBAC)
- ✅ BCrypt şifre hashleme
- ✅ Hesap kilitleme (5 başarısız deneme)
- ✅ HTTP interceptor ile token yönetimi

### Altyapı
- ✅ Redis önbellekleme
- ✅ Kafka event streaming
- ✅ Resilience4J circuit breaker
- ✅ Zipkin dağıtık izleme
- ✅ Prometheus + Grafana monitoring
- ✅ Consul servis keşfi
- ✅ Flyway veritabanı migration

---

## 🚀 Kurulum

### Gereksinimler

- Java 17+
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+

### 1. Projeyi Klonlayın

```bash
git clone https://github.com/m-ogur34/autohub.git
cd autohub
```

### 2. Yerel Geliştirme (Manuel)

**Backend:**
```bash
cd backend
# Bağımlılıkları indir ve derle
mvn clean install -DskipTests

# Uygulamayı başlat
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
# Bağımlılıkları yükle
npm install

# Geliştirme sunucusunu başlat
npm start
```

Uygulama şu adreslerde çalışır:
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- Actuator: http://localhost:8080/api/actuator/health

---

## 🐳 Docker ile Çalıştırma

Tüm servisleri tek komutla başlatın:

```bash
# Tüm servisleri başlat (arka planda)
docker-compose up -d

# Logları görüntüle
docker-compose logs -f backend

# Servislerin durumunu kontrol et
docker-compose ps

# Durdur
docker-compose down

# Durdur ve volume'ları sil (veri temizleme)
docker-compose down -v
```

### Servis Adresleri

| Servis | Adres |
|--------|-------|
| Frontend | http://localhost:80 |
| Backend API | http://localhost:8080/api |
| Grafana | http://localhost:3000 (admin/admin123) |
| Prometheus | http://localhost:9090 |
| Consul UI | http://localhost:8500 |
| Zipkin UI | http://localhost:9411 |
| ActiveMQ Konsol | http://localhost:8161 (admin/admin) |
| Elasticsearch | http://localhost:9200 |

---

## ☸️ Kubernetes ile Dağıtım

```bash
# Namespace oluştur
kubectl apply -f k8s/namespace.yaml

# Veritabanı dağıt
kubectl apply -f k8s/postgres-deployment.yaml

# Backend dağıt
kubectl apply -f k8s/backend-deployment.yaml

# Frontend dağıt
kubectl apply -f k8s/frontend-deployment.yaml

# Tüm pod'ların durumunu kontrol et
kubectl get pods -n autohub

# Backend loglarını görüntüle
kubectl logs -f deployment/autohub-backend -n autohub
```

---

## 📖 API Dokümantasyonu

### Kimlik Doğrulama

```http
# Giriş
POST /api/auth/login
Content-Type: application/json
{
  "username": "admin",
  "password": "Admin@123!"
}

# Kayıt
POST /api/auth/register
Content-Type: application/json
{
  "username": "kullanici",
  "email": "kullanici@ornek.com",
  "password": "Sifre@123",
  "confirmPassword": "Sifre@123"
}

# Token Yenileme
POST /api/auth/refresh
X-Refresh-Token: <refresh_token>
```

### Araç Endpoint'leri

```http
# Tüm araçları listele
GET /api/vehicles?page=0&size=10&sort=price,asc

# Araç detayı
GET /api/vehicles/{id}

# Yeni araç ekle (ADMIN/MANAGER)
POST /api/vehicles
Authorization: Bearer <token>

# Araç güncelle
PUT /api/vehicles/{id}
Authorization: Bearer <token>

# Araç sil
DELETE /api/vehicles/{id}
Authorization: Bearer <token>

# Durum güncelle
PATCH /api/vehicles/{id}/status?status=RENTED
Authorization: Bearer <token>

# Arama
GET /api/vehicles/search?q=toyota+corolla

# İstatistikler
GET /api/vehicles/stats
```

---

## 🔐 Ortam Değişkenleri

Backend için `.env` dosyası:

```env
# Veritabanı
DB_HOST=localhost
DB_PORT=5432
DB_NAME=autohub_db
DB_USERNAME=autohub_user
DB_PASSWORD=autohub_pass

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# Kafka
KAFKA_SERVERS=localhost:9092

# Elasticsearch
ES_URI=http://localhost:9200

# JWT (minimum 32 karakter, base64 encoded)
JWT_SECRET=YXV0b2h1Yi1zdXBlci1zZWNyZXQta2V5

# JWT süreleri (ms)
JWT_EXPIRATION=86400000
JWT_REFRESH_EXP=604800000
```

---

## 👤 Varsayılan Kullanıcılar

| Kullanıcı Adı | Şifre | Rol |
|--------------|-------|-----|
| admin | Admin@123! | ADMIN |

---

## 📊 Grafana Dashboard

Grafana'ya erişin: http://localhost:3000

- Kullanıcı: `admin`
- Şifre: `admin123`

Spring Boot uygulaması metrikleri otomatik olarak Prometheus'a gönderilir ve Grafana'da görselleştirilir.

---

## 🔄 OOP Prensipleri

Bu projede nesne yönelimli programlama prensipleri şu şekilde uygulanmıştır:

| Prensip | Uygulama |
|---------|----------|
| **Soyutlama** | `VehicleService` arayüzü, veritabanı detaylarını gizler |
| **Kapsülleme** | Entity sınıflarında `private` alanlar, getter/setter ile erişim |
| **Kalıtım** | Tüm Entity'ler `BaseEntity`'den miras alır |
| **Polimorfizm** | `UserDetails` implementasyonu, enum tabanlı davranış |

---

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

---

## Öğrenilen Kavramlar

### Spring Boot Katmanlı Mimari
```
Controller  → HTTP mapping, input validation, response format
Service     → İş mantığı, @Transactional sınırı
Repository  → Spring Data JPA, JPQL, Specification
Entity      → JPA ORM, ilişkiler, BaseEntity (Auditing)

Bağımlılık yönü (tek yön):
Controller → Service → Repository → DB
Hiçbir zaman Repository → Service çağırmamalı
```

### JPA İlişkileri
```java
// OneToMany: Araç → Kiralama geçmişi
@OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY,
           cascade = CascadeType.ALL, orphanRemoval = true)
private List<Transaction> transactions = new ArrayList<>();

// ManyToOne: Kiralama → Araç (FK: transactions.vehicle_id)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "vehicle_id")
private Vehicle vehicle;

// FetchType.LAZY: transaction yüklenince araç yüklenmez
// transaction.getVehicle() çağrılınca ayrı SQL atılır
// N+1 sorunu: 100 transaction → 101 SQL (her biri için getVehicle())
// Çözüm: @EntityGraph veya JOIN FETCH sorgusu
```

### Spring Security (JWT)
```
İstek akışı:
  HTTP Request
      ↓
  JwtAuthenticationFilter
      ├── Authorization: Bearer <token> header var mı?
      ├── JWT imzası geçerli mi? (HMAC-SHA256)
      ├── Token süresi dolmadı mı?
      ├── UserDetailsService.loadUserByUsername(email)
      └── SecurityContextHolder'a Authentication koy
      ↓
  Controller (@PreAuthorize / hasRole kontrolleri)

Stateless Session:
  SessionCreationPolicy.STATELESS → sunucuda session tutulmaz
  Her istek kendi JWT'sini getirir → yatay ölçekleme kolay
```

### Redis Kullanım Alanları
```
Cache (@Cacheable):
  "vehicles"   → tüm araç listesi (TTL: 5dk)
  "vehicle"    → tek araç detayı (TTL: 10dk)
  @CacheEvict → araç güncellenince ilgili cache temizlenir

Session:
  Kullanıcı oturum bilgisi → JWT blacklist (logout sonrası)

Rate Limiter:
  "rate-limit:{ip}" → Redis INCR ile saniye/dakika bazlı limit
```

### Kafka Event-Driven
```
Producer (araç kiralama tamamlandı):
  kafkaTemplate.send("vehicle-events", RentalCompletedEvent)

Consumer (notification-service):
  @KafkaListener(topics = "vehicle-events", groupId = "notification-group")
  public void handleRentalCompleted(RentalCompletedEvent event) {
      // email/SMS gönder
  }

Avantaj: Servisler birbirinden bağımsız, async işleme
```

### Elasticsearch Full-Text Search
```
Araç arama:
  - Marka, model, açıklama alanlarında tam metin arama
  - Multi-match query: birden fazla alana aynı anda ara
  - Türkçe analyzer: "otomobil" → "otomobil", "araç" stem'i
  - Aggregation: marka bazında araç sayısı, fiyat istatistikleri
```

---

## Mülakat Soruları

**Q: @Cacheable, @CachePut, @CacheEvict farkları nedir?**
A: `@Cacheable`: Metod sonucunu cache'e yazar; aynı key ile tekrar çağrılınca metod çalıştırılmaz, cache'den döner. `@CachePut`: Her zaman metodu çalıştırır ve sonucu cache'e yazar (cache güncelleme — update sonrası). `@CacheEvict`: Cache'i temizler (entity silindiğinde veya güncellendiğinde). `allEntries=true` ile ilgili tüm cache'i temizler. Birlikte örnek: araç güncelleme → `@CachePut("vehicle")` tek araç günceller, `@CacheEvict("vehicles")` liste cache'ini temizler.

**Q: N+1 sorunu nedir ve nasıl çözülür?**
A: N+1: Ana listeyi getirmek için 1 SQL, her eleman için ilişkili nesneyi getirmek için N SQL. Örnek: 100 araç listesi + her aracın markası = 101 SQL. Çözüm 1: `@EntityGraph` — ilgili entity JOIN ile tek sorguda gelir. Çözüm 2: JPQL `JOIN FETCH` — `SELECT v FROM Vehicle v JOIN FETCH v.brand`. Çözüm 3: `@BatchSize` — N+1 yerine N/batchSize SQL. Spring Data JPA'da test etmek için `spring.jpa.show-sql=true` ile SQL sayısı sayılabilir.

**Q: JPA Lazy Loading nasıl çalışır? Transaction dışında sorun olur mu?**
A: LAZY: entity yüklendiğinde ilişkili nesne Hibernate proxy'si olarak atanır. Gerçek veriye erişilince (proxy.getId() hariç) SQL atılır. Problem: Transaction kapandıktan sonra erişilirse `LazyInitializationException`. Çözüm: `@Transactional(readOnly=true)` ile servis metodunu transaction'lı yap, DTO'ya lazy field'ı kopyala, sonra transaction dışına çık. Alternatif: OpenEntityManagerInView (antipattern — Controller'da transaction — tercih edilmez).

**Q: Spring Security filter chain sırası neden önemli?**
A: JwtAuthenticationFilter'ı `UsernamePasswordAuthenticationFilter`'dan önce ekleriz. Sıra yanlış olursa JWT kontrolü yapılmadan form login denenebilir. `.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` doğru sıra. SecurityFilterChain'de `authorizeHttpRequests` sıraması da önemli: daha spesifik path'ler önce, `anyRequest()` en sona.

**Q: Soft delete nedir? Neden tercih edilir?**
A: `is_deleted = true` ile kaydı veritabanından silmek yerine pasif işaretleme. Avantajlar: silinen veriler geri alınabilir (audit trail), raporlama için geçmiş veriler erişilebilir, referential integrity sorunları önlenir (FK constraints bozulmaz). Dezavantaj: tablo büyür, tüm sorguları `WHERE is_deleted = false` ile filtreleme gerekir. Spring Data ile: `@Where(clause = "is_deleted = false")` annotation ile otomatik filtreleme.

**Q: Kafka Producer ve Consumer hangi garantileri verir?**
A: Producer: `acks=all` → tüm replica'lar teyit edene kadar bekler (en güvenli). `acks=1` → sadece leader onaylar. `acks=0` → onay bekleme (en hızlı, kayıp riski). Consumer: `auto-commit=false` + manuel ack → mesaj işlendikten sonra commit, hata durumunda yeniden işleme. `enable.auto.commit=true` → her `poll()` sonrası commit (kayıp riski). Exactly-once: Kafka Transactions API + idempotent consumer gerektirir.

**Q: @EnableAsync ile @Async nasıl çalışır?**
A: `@EnableAsync` Spring'e async method execution etkinleştir der. `@Async` ile işaretlenen metod çağrıldığında, aynı thread'de çalışmaz — Spring thread pool'dan bir thread alır ve metodu arka planda çalıştırır. Çağıran thread anında döner (non-blocking). Return type `void` veya `CompletableFuture<T>` olabilir. Dikkat: `@Async` kendi class'ından çağrılırsa çalışmaz (self-invocation) — Spring AOP proxy üzerinden geçmesi gerekir.

**Q: Specification Pattern ne zaman kullanılır?**
A: Dinamik filtreleme senaryolarında: "marka=BMW, yıl=2020-2023, fiyat<500TL/gün, aktif araçlar" gibi kombinasyonlar. Her filtre bir Specification. `Specification.where(byBrand).and(byYear).and(byPrice)` ile birleştirilir. JpaSpecificationExecutor.findAll(spec, pageable) çağrılır. SQL dinamik olarak üretilir. Alternatif: QueryDSL (tip güvenli, daha güçlü) veya JPQL ile elle yazma (esnek değil).
