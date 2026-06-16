# 04 — Design Patterns
**Zorluk:** Intermediate (5/10) | **Java 21** | **Mülakat Hazırlığı**

Java 21 ile 20+ tasarım deseni sıfırdan implementasyon. Her pattern için gerçek dünya senaryosu ve mülakat soruları.

---

## İçerik

### Creational Patterns (Nesne Oluşturma)
| Pattern | Dosya | Kullanım |
|---------|-------|---------|
| **Singleton** | `creational/singleton/SingletonPatterns.java` | Eager, DCL, Bill Pugh, Enum |
| **Factory Method & Abstract Factory** | `creational/factory/FactoryPatterns.java` | Notification, DB Factory |
| **Builder** | `creational/builder/BuilderPattern.java` | HttpRequest, SqlQueryBuilder, Record |

### Structural Patterns (Yapısal)
| Pattern | Dosya | Kullanım |
|---------|-------|---------|
| **Decorator** | `structural/decorator/DecoratorPattern.java` | Encryption/Compression/Logging/Caching zinciri |
| **Proxy** | `structural/proxy/ProxyPattern.java` | Virtual, Protection, Caching, JDK Dynamic Proxy |

### Behavioral Patterns (Davranışsal)
| Pattern | Dosya | Kullanım |
|---------|-------|---------|
| **Strategy** | `behavioral/strategy/StrategyPattern.java` | Ödeme, Fiyatlandırma, Sıralama |
| **Observer** | `behavioral/observer/ObserverPattern.java` | Sipariş Olayları, Borsa Takibi |
| **Chain of Responsibility** | `behavioral/chain/ChainOfResponsibility.java` | HTTP Pipeline, Filter Chain |

### Enterprise Patterns
| Pattern | Dosya | Kullanım |
|---------|-------|---------|
| **Repository + CQRS + Unit of Work** | `enterprise/RepositoryCQRS.java` | Sealed interface, Record, switch expression |

---

## Pattern Seçim Kılavuzu

```
Nesne oluşturma karmaşıksa       → Builder
Tek örnek gerekiyorsa             → Singleton (Bill Pugh)
Hangi sınıf bilinmiyorsa          → Factory Method
İlgili nesne ailesi gerekiyorsa   → Abstract Factory
Davranış runtime'da değişecekse   → Strategy
Birden fazla nesne bildirim alacaksa → Observer
Dinamik davranış eklenmesi gerekse → Decorator
Erişim kontrolü/önbellek gerekiyorsa → Proxy
İstek zincirde işlenecekse       → Chain of Responsibility
Okuma/yazma ayrımı gerekiyorsa    → CQRS
```

---

## Mülakat Soruları

**Q: Singleton'ı thread-safe nasıl yazarsın? volatile neden gerekli?**
A: Bill Pugh (en iyi): `private static class Holder { static final Singleton INSTANCE = new Singleton(); }` — JVM class loading thread-safe, lazy initialization. DCL (Double-Checked Locking): `volatile` olmadan CPU/compiler instruction reorder yapabilir → yarı initialize nesne görülebilir. `volatile` görünürlüğü ve reorder'ı engeller. En sade: Enum Singleton — serialization ve reflection'a karşı da güvenli.

**Q: Factory Method ve Abstract Factory farkı nedir?**
A: Factory Method: Tek bir ürün oluştururken subclass hangi sınıfın yaratılacağını belirler. `NotificationFactory.createNotification()` → EmailNotification veya SMSNotification. Abstract Factory: İlişkili ürün ailesi üretir. `DatabaseFactory` → `createConnection() + createCommand() + createReader()` — MySQLFactory veya OracleFactory. Kural: Tek ürün → Factory Method, ürün ailesi → Abstract Factory.

**Q: Decorator ve Proxy farkı nedir?**
A: Proxy: Aynı interface, erişim kontrolü veya lazy loading amacıyla — nesnenin yerine geçer. Müşteri proxy'den habersiz. Örnekler: JDK Dynamic Proxy (@Transactional), Hibernate lazy entity. Decorator: Aynı interface, davranış ekleme amacıyla — nesneyi sarar ve genişletir. Müşteri genellikle decorator olduğunu bilir. Örnekler: BufferedReader(FileReader), LoggingDecorator(CachingDecorator(Service)). Fark: Proxy kontroller, Decorator ekler.

**Q: Strategy ve State pattern farkı?**
A: Strategy: Algoritma dışarıdan enjekte edilir, client seçer. `paymentService.setStrategy(new CreditCardStrategy())`. Nesne state'i değişmez — sadece davranış değişir. State: Nesne kendi state'ini yönetir, state'e göre davranış değişir. `order.setState(new ShippedState())` — State kendi geçişini tetikler. Fark: Strategy müşteri tarafından kontrol edilir, State nesne tarafından.

**Q: Spring'de hangi tasarım desenleri kullanılır?**
A: Proxy → `@Transactional`, `@Cacheable`, `@Async` — Spring AOP proxy ile. Factory → `BeanFactory`, `ApplicationContext` — bean üretimi. Singleton → Spring bean'ler default singleton scope. Observer → `ApplicationEventPublisher` + `@EventListener`. Template Method → `JdbcTemplate`, `RestTemplate`. Decorator → `HttpMessageConverter` zincirleme. Composite → `HandlerInterceptorChain`. Builder → `ResponseEntity.builder()`, `MockMvc.perform()`.

**Q: @Transactional hangi pattern'i kullanır?**
A: Proxy Pattern (AOP tabanlı). `@Transactional` ile işaretlenen sınıf Spring container'ında Proxy nesnesiyle sarılır (JDK Dynamic Proxy veya CGLIB). Çağrı gelince proxy: (1) Transaction başlat, (2) Gerçek metodu çağır, (3) Başarılıysa commit, hata varsa rollback. Sınıf içinden kendi metodunu çağırırken proxy bypass olur — self-invocation problemi. Çözüm: `AopContext.currentProxy()` veya ayrı sınıfa taşı.

**Q: CQRS ne zaman kullanılır?**
A: Command Query Responsibility Segregation: Okuma (Query) ve yazma (Command) modelleri ayrılır. Ne zaman: Okuma ve yazma yük dengesizliği (10:1 okuma ağırlıklı). Karmaşık sorgu gereksinimleri (birden fazla tablo, projeksiyon). Yatay ölçekleme: okuma replika'larından, yazma master'dan. Dezavantaj: İki model yönetmek karmaşıklık artırır, eventual consistency. Bu projede: `RepositoryCQRS.java` — `IProductRepository` (command) + `IProductQueryService` (query) ayrımı.

---

**Önceki →** [03 - Java Concurrency](../03_JAVA-CONCURRENCY)
**Sonraki →** [05 - Student Tracker JDBC](../05_STUDENT-TRACKER)
