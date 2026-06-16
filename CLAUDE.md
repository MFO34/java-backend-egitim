# Mami Eğitim — Claude Code Bağlam Dosyası

Bu dosya, herhangi bir Claude Code instance'ının bu projeye bağlandığında
Fatih'i ve öğrenme yolculuğunu anında anlayabilmesi için yazılmıştır.
Yeni bir konuşmada bu dosyayı oku — sıfırdan tanışmana gerek yok.

---

## Fatih Kimdir?

- Hedef: Sağlam bir **backend mimarı** olmak
- Java bilgisi var ama "neyin neden olduğunu" tam kavramak istiyor
- Günde ~10 saat çalışıyor
- Repoları bitirince kendi projelerini yazmayı planlıyor
- Öğrenme tarzı: sezgisel kavrama, "neden böyle?" sorusu öncelikli

---

## Nasıl Öğretirim — Kurallar

1. **Her zaman "neden" ile başla.** Kodu göstermeden önce problemi anlat.
2. **Alternatif karşılaştır.** "Böyle yapmasaydık ne olurdu?" sorusunu sor.
3. **Gerçek dünya bağlantısı kur.** Banka, fatura, sipariş — somut senaryo.
4. **Her açıklamanın sonuna "Not defteri özeti:" ekle.** 4-6 madde, kısa, elle yazılabilir.
5. **Fatih'e soru sor.** Her konu sonunda bir soru bırak, cevabını bekle.
6. Emoji kullanma, gereksiz süsleme yapma. Sade ve net ol.

---

## Repo Yapısı

`c:\Users\fatih\...\Mami Eğitim\` altında 24 sıralı repo var:

| No | Repo | Zorluk | Konu |
|----|------|--------|------|
| 01 | DSA-FUNDAMENTALS | Beginner 3/10 | Arrays, LinkedList, Tree, Graph, Sorting, DP |
| 02 | BANK-ACCOUNT-MANAGER | Beginner 2/10 | Core Java OOP, Collections, Streams, Lambdas |
| 03 | JAVA-CONCURRENCY | Intermediate 6/10 | Threads, Executors, CompletableFuture, Virtual Threads |
| 04 | DESIGN-PATTERNS | Intermediate 5/10 | Creational, Structural, Behavioral |
| 05 | STUDENT-TRACKER | Beginner 3/10 | JDBC, DAO, Records, Sealed Classes, PostgreSQL |
| 06 | POSTGRESQL-ADVANCED | Intermediate 6/10 | Window functions, CTE, Index, PLPGSQL |
| 07 | ORACLE-PLSQL | Intermediate 6/10 | PL/SQL, Packages, Cursors, Triggers |
| 08 | MONGODB | Intermediate 5/10 | Document model, Aggregation, Indexing, Spring Data |
| 09 | PARKING-LOT-MANAGER | Beginner 4/10 | Spring Boot, REST API, CRUD |
| 10 | ECOMMERCE-API | Intermediate 5/10 | JPA, Hibernate, Relationships, Liquibase |
| 11 | TESTING-GUIDE | Intermediate 6/10 | JUnit5, Mockito, Testcontainers, TDD |
| 12 | AUTH-SERVICE | Intermediate 6/10 | Spring Security, JWT, OAuth2, Redis |
| 13 | CACHE-DEMO | Intermediate 6/10 | Redis, Caching, Session, PubSub, Rate Limiting |
| 14 | ACTIVEMQ-MESSAGING | Intermediate 5/10 | JMS, Queue, Topic, Spring JMS, DLQ |
| 15 | KAFKA-DEEP-DIVE | Advanced 8/10 | Streams, Consumer Groups, Schema Registry, DLQ |
| 16 | ELASTICSEARCH-DEEP-DIVE | Intermediate 7/10 | Query DSL, Aggregations, Analyzers, Mapping |
| 17 | CONTAINERIZATION-GUIDE | Intermediate 7/10 | Docker, Nginx, Multistage Build, Compose |
| 18 | CICD-PIPELINE | Intermediate 6/10 | GitHub Actions, Docker, Build, Test, Deploy |
| 19 | ANGULAR-UI | Intermediate 7/10 | Components, RxJS, NgRx, Routing, Forms, Signals |
| 20 | AUTOHUB-PLATFORM | Advanced 8/10 | Fullstack monolith: Spring + Angular + Kafka + ES |
| 21 | ECOMMERCE-HUB | Advanced 9/10 | Microservices: Spring Cloud + Kafka + ES + Consul |
| 22 | DIGITAL-BANK | Senior 10/10 | Microservices + Kubernetes + Angular |
| 23 | SPRING-WEBFLUX | Advanced 8/10 | Reactor, Mono, Flux, WebClient, R2DBC |
| 24 | SYSTEM-DESIGN | Senior 9/10 | URL Shortener, Rate Limiter, Chat, Distributed |

---

## Öğrenme Sırası (Planlanan)

```
02 → 01 → 04 → 03 → 05 → 09 → 10 → 11 → 12 → 13 → 14 → 15 → 16 → 17 → 18 → 19 → 20 → 21 → 22 → 23 → 24
```

Neden 02 önce: Core Java OOP'u gerçek senaryo üzerinde pekiştirmek için.

---

## Şu Anki Durum

**Aktif repo: 02 — BANK-ACCOUNT-MANAGER**

### Tamamlanan Konular (02 içinde)

**Model katmanı:**
- `abstract class` — neden `new Account()` kurulamaz
- `interface` vs `abstract class` — "ne olduğu" vs "ne yapabildiği"
- `final` field — immutability, constructor'da tek atama
- `static` sayaç — sınıfa ait tek kopya, nesneye değil
- Defensive copy — `return new ArrayList<>(list)` neden, referans tehlikesi
- `protected` vs `public` setter — en az yetki prensibi
- `private static final` — sınıf sabiti kalıbı
- Runtime polymorphism / Dynamic Dispatch — `checkWithdrawEligibility` örneği
- Single Responsibility Prensibi — her sınıf kendi kuralını taşır
- Enum — tip güvenliği, enum'un aslında class olması, field + constructor

**Sıradaki konu:**
- `AccountService.java` — Stream API, Lambda, Collections

---

## Önemli Kavramlar Sözlüğü (İşlenenler)

| Kavram | Kısa Açıklama |
|--------|---------------|
| abstract class | Doğrudan örneklenemeyen, ortak davranışları toplayan sınıf |
| interface | "Yetenek" sözleşmesi, çoklu implement edilebilir |
| final field | Constructor'da bir kez atanır, sonra değiştirilemez |
| static | Nesneye değil sınıfa ait, bellekte tek kopya |
| private static final | Sınıf sabiti kalıbı — sabit değerler için standart |
| Defensive copy | Referans sızdırmamak için koleksiyonun kopyasını döndürmek |
| protected | Bu sınıf + alt sınıflar + aynı paket erişebilir |
| Dynamic Dispatch | Hangi metodun çalışacağı runtime'da nesnenin gerçek tipine göre belirlenir |
| Single Responsibility | Her sınıf tek bir şeyden sorumlu |
| Enum | Sabit değer kümesi, aslında class, field ve metod taşıyabilir |
| Least Privilege | Erişimi her zaman ihtiyaç kadar aç, fazlasını değil |
