# 🎓 Öğrenci Takip Sistemi

> Java 21 + JDBC + PostgreSQL ile geliştirilmiş konsol tabanlı öğrenci yönetim uygulaması.
> DAO Pattern, HikariCP, Sealed Classes, Records ve gelişmiş SQL öğretilmiştir.

---

## 📋 Proje Açıklaması

Gerçek bir okul yönetim sistemini simüle eder. Öğrenci, öğretmen, ders ve not yönetimi yapar.
Saf JDBC kullanılarak ORM (Hibernate/JPA) olmadan veritabanı işlemleri gösterilmiştir.

---

## 🗄️ Veritabanı Şeması (ER Diyagram)

```
┌─────────────┐        ┌─────────────────┐        ┌───────────────┐
│  teachers   │        │    courses      │        │   students    │
├─────────────┤        ├─────────────────┤        ├───────────────┤
│ id (PK)     │◄───────│ teacher_id (FK) │        │ id (PK)       │
│ first_name  │        │ id (PK)         │        │ first_name    │
│ last_name   │        │ course_code     │        │ last_name     │
│ email       │        │ course_name     │        │ email         │
│ department  │        │ credits         │        │ birth_date    │
│ salary      │        │ capacity        │        │ national_id   │
│ is_active   │        │ is_active       │        │ grade_level   │
└─────────────┘        └────────┬────────┘        └───────┬───────┘
                                │                         │
                                │    ┌────────────────┐   │
                                └────│  enrollments   │───┘
                                     ├────────────────┤
                                     │ id (PK)        │
                                     │ student_id (FK)│
                                     │ course_id (FK) │
                                     │ enrolled_at    │
                                     │ status         │
                                     └───────┬────────┘
                                             │
                                     ┌───────▼────────┐
                                     │    grades      │
                                     ├────────────────┤
                                     │ id (PK)        │
                                     │enrollment_id(FK│
                                     │ grade_type     │
                                     │ score          │
                                     │ graded_at      │
                                     └────────────────┘
```

---

## 📁 Proje Yapısı

```
ogrenci-takip-sistemi/
│
├── docker-compose.yml              → Sadece PostgreSQL container
├── pom.xml                         → Maven + Java 21 + JDBC bağımlılıkları
├── .gitignore
│
└── src/main/
    ├── java/com/ogrenci/
    │   ├── app/
    │   │   ├── OgrenciApp.java     → main(), bağımlılık kurulumu
    │   │   └── ConsoleMenu.java    → Menü (Pattern Matching for switch)
    │   │
    │   ├── model/                  → Java 21 Record modeller
    │   │   ├── Student.java        → record + Builder + compact constructor
    │   │   ├── Teacher.java        → record + Builder
    │   │   ├── Course.java         → record + Builder
    │   │   ├── Enrollment.java     → record + Builder
    │   │   ├── Grade.java          → record + letterGrade() metodu
    │   │   └── QueryResult.java    → sealed interface (Success, Empty, Failure)
    │   │
    │   ├── dao/                    → DAO Pattern — tüm SQL burada
    │   │   ├── StudentDAO.java     → CRUD + Batch + JOIN raporu
    │   │   ├── TeacherDAO.java     → CRUD + departman filtresi
    │   │   ├── CourseDAO.java      → CRUD + LEFT JOIN + öğretmen filtresi
    │   │   ├── EnrollmentDAO.java  → Kayıt/iptal + 3-tablo JOIN
    │   │   └── GradeDAO.java       → CRUD + Batch + HAVING sorguları
    │   │
    │   ├── service/                → İş mantığı katmanı
    │   │   ├── StudentService.java → Stream API groupingBy, QueryResult
    │   │   ├── CourseService.java  → Kapasite kontrolü, öğretmen-ders
    │   │   └── GradeService.java   → Batch girişi, rapor metodları
    │   │
    │   ├── exception/              → Custom exception'lar
    │   │   ├── DatabaseException.java
    │   │   ├── StudentNotFoundException.java
    │   │   └── CourseNotFoundException.java
    │   │
    │   └── util/
    │       ├── DatabaseConnection.java  → Singleton + HikariCP Pool
    │       ├── DatabaseInitializer.java → Şema + veri yükleme (Transaction)
    │       └── ConsoleHelper.java       → Kullanıcı girişi yardımcısı
    │
    └── resources/
        ├── schema.sql      → CREATE TABLE, INDEX, TRIGGER
        ├── data.sql        → Örnek veriler
        ├── queries.sql     → Eğitim amaçlı SQL örnekleri
        └── hikari.properties → HikariCP ayarları
```

---

## ⚙️ Kurulum ve Çalıştırma

### Gereksinimler
- Java 21 LTS
- Apache Maven 3.6+
- Docker + Docker Desktop

### 1. PostgreSQL'i Docker ile Başlat (SADECE DB)
```bash
# Sadece db servisini başlat (uygulama Docker'da değil!)
docker-compose up -d db

# Durum kontrol
docker-compose ps

# Logları izle
docker-compose logs -f db
```

### 2. Bağlantıyı Test Et
```bash
docker exec -it ogrenci_db psql -U ogrenci_user -d ogrenci_takip
# PostgreSQL prompt açılıyorsa hazır
\q
```

### 3. Derle
```bash
mvn clean package -DskipTests
```

### 4. Çalıştır
```bash
java --enable-preview -jar target/ogrenci-takip.jar
```

---

## 🔍 JDBC vs JPA Karşılaştırması

| Özellik | JDBC (Bu Proje) | JPA/Hibernate |
|---|---|---|
| SQL kontrolü | Tam kontrol | ORM otomatik üretir |
| Öğrenme eğrisi | Düşük | Yüksek |
| Performans | Optimize edilebilir | N+1 sorunu riski |
| Boilerplate kod | Fazla | Az |
| Karmaşık sorgu | Kolay | JPQL/Native query |
| Ne zaman kullanılır | Performans kritik, SQL öğrenmek | Hızlı geliştirme |

---

## 🚀 HikariCP Nedir? Neden Kullanılır?

**Connection Pool = Bağlantı Havuzu**

```
Havuzsuz:  Her sorgu → Bağlantı aç (50ms) → SQL çalıştır → Bağlantı kapat
Havuzlu:   Her sorgu → Havuzdan al (1ms)  → SQL çalıştır → Havuza iade et

HikariCP: Java'nın en hızlı connection pool kütüphanesi
- Uygulama başlangıcında N bağlantı açar (minimumIdle=2)
- Yük altında otomatik büyür (maximumPoolSize=10)
- Ölü bağlantıları tespit edip yeniler
```

---

## 🏗️ DAO Pattern Açıklaması

```
         ┌──────────────┐
         │  ConsoleMenu │  ← Kullanıcı arayüzü
         └──────┬───────┘
                │ çağırır
         ┌──────▼───────┐
         │   Service    │  ← İş mantığı (validation, workflow)
         └──────┬───────┘
                │ çağırır
         ┌──────▼───────┐
         │     DAO      │  ← Sadece SQL (veritabanı işlemleri)
         └──────┬───────┘
                │ JDBC
         ┌──────▼───────┐
         │  PostgreSQL  │  ← Veritabanı
         └──────────────┘
```

**Neden bu katmanlı yapı?**
- DAO'yu değiştirirsen (JDBC → JPA) Service ve Menu etkilenmez
- Service'i test ederken DAO'yu mock'layabilirsin
- Her katman tek sorumluluğa sahip (Single Responsibility)

---

## 📖 Java 21 Özellikleri

### Record
```java
// Geleneksel: ~60 satır
// Record: 1 satır — constructor, getter, equals, hashCode, toString otomatik
public record Student(int id, String firstName, String email, ...) {}
```

### Sealed Interface + Pattern Matching
```java
// Sealed: sadece izin verilen tipler implement edebilir
public sealed interface QueryResult<T>
    permits QueryResult.Success, QueryResult.Empty, QueryResult.Failure {}

// Pattern Matching for switch (Java 21)
switch (result) {
    case QueryResult.Success<Student> s -> System.out.println(s.data().fullName());
    case QueryResult.Failure<Student> f -> System.out.println("Hata: " + f.message());
    default -> {}
}
```

### var (Tip Çıkarımı)
```java
var students = new ArrayList<Student>(); // List<Student> yazmak zorunda değiliz
var conn = dbConn.getConnection();       // Connection tipini tekrar yazmaya gerek yok
```

---

## 🛠️ Teknolojiler

| Teknoloji | Versiyon | Amaç |
|---|---|---|
| Java | 21 LTS | Ana dil (Record, Sealed, Pattern Matching) |
| JDBC | Java standart | Veritabanı bağlantı API'si |
| PostgreSQL | 16 | İlişkisel veritabanı |
| HikariCP | 5.1.0 | Yüksek performanslı connection pool |
| Maven | 3.6+ | Bağımlılık yönetimi |
| Docker | 24+ | PostgreSQL container |

---

---

## Mülakat Soruları

**Q: JDBC nedir ve JPA/Hibernate'den farkı nedir?**
A: JDBC (Java Database Connectivity): Java'nın standart DB erişim API'si. SQL'i kendin yaz, ResultSet'ten manuel map et, Connection'ı kendin yönet. JPA/Hibernate: ORM (Object-Relational Mapping) — Java nesneleri otomatik tablolara map edilir, SQL üretilir, cache ve lazy loading gibi özellikler hazır. Fark: JDBC daha düşük seviye, daha fazla kontrol, daha az otomatizasyon. JPA daha üretken, daha az SQL, ama daha az kontrol. Büyük projelerde genellikle JPA, performans kritik sorgularda Native SQL veya JDBC.

**Q: Connection Pool (HikariCP) neden gereklidir?**
A: Her DB sorgusu için yeni bağlantı açmak: TCP handshake + DB authentication = 20-50ms overhead. Yüksek yük altında binlerce bağlantı → DB bağlantı limiti aşılır. HikariCP: uygulama başlangıcında N bağlantı açar (minimumIdle=5), sorgu gelince havuzdan alır (< 1ms), işlem bitince iade eder. Avantajlar: latency düşer, DB korunur, bağlantı sayısı sınırlı tutulur. HikariCP en hızlı Java connection pool: `benchmark: HikariCP >> C3P0 >> DBCP`.

**Q: try-with-resources JDBC'de neden kritiktir?**
A: JDBC kaynakları (Connection, PreparedStatement, ResultSet) close() edilmezse: bağlantı havuzuna iade edilmez → havuz dolar → yeni istek gelince bekler → deadlock. try-with-resources ile: `try (Connection conn = ...; PreparedStatement ps = ...) { }` — blok bitince otomatik close(). Exception fırlatılsa bile close() garantilenir. Finally bloğuna gerek kalmaz. AutoCloseable interface'ini implement eden her kaynak için kullanılabilir.

**Q: PreparedStatement neden Statement'tan üstündür?**
A: Statement: `"SELECT * FROM students WHERE email = '" + email + "'"` — SQL Injection riski! Kullanıcı `email = "' OR '1'='1"` girerse tüm veriler açılır. PreparedStatement: `"SELECT * FROM students WHERE email = ?"` — parametreler ayrı gönderilir, SQL'e katıştırılmaz. DB tarafında pre-compile: aynı query defalarca çalışacaksa plan cache — performans artışı. `ps.setString(1, email)` tip güvenli set.

**Q: DAO (Data Access Object) Pattern neden kullanılır?**
A: DAO: veri erişim mantığını iş mantığından ayırır. `StudentDAO` sınıfı tüm DB işlemlerini içerir, `StudentService` DAO'yu çağırır — SQL'i bilmez. Avantajlar: DB değişirse sadece DAO değişir (PostgreSQL → Oracle → geçiş kolay). Service katmanı test edilirken MockDAO kullanılabilir. Kod tekrarı önlenir. SOLID Single Responsibility: her katman tek sorumlu. Spring Data JPA DAO pattern'ın modern implementasyonu — interface yazarsın, Spring üretir.

**Q: Record neden DTO için idealdir?**
A: Record (Java 16+): `public record StudentDTO(int id, String name, String email) {}` — constructor, getter (id(), name()), equals/hashCode/toString otomatik üretilir. Immutable (değiştirilemez) — field değerleri constructor dışında değiştirilemez. DTO'lar zaten sadece veri taşır, değiştirilmez olması güvenlidir. Compact constructor ile validation: `public StudentDTO { if (email == null) throw ... }`. Normal class'a göre ~50 satır daha az kod.

**Q: Sealed interface ne sağlar?**
A: `sealed interface QueryResult<T> permits Success, Empty, Failure` — sadece izin verilen tipler implement edebilir. Switch expression ile exhaustiveness: tüm durumlar ele alınmadıysa compile hatası. `case QueryResult.Success<T> s -> ...` pattern matching ile tip dönüştürme gereksiz. Alternatif: Optional veya null — tip bilgisi kaybolur. Exception — kontrol akışı için exception kötü pratik. Sealed interface: functional error handling için ideal.

**Q: Singleton Pattern neden HikariCP için kullanılır?**
A: HikariCP başlatmak pahalı: TCP bağlantıları kurulur, thread'ler oluşturulur, konfigürasyon yüklenir (~2-3 saniye). Uygulama boyunca sadece bir DataSource gerekir. Singleton: `private static volatile HikariDataSource instance` — tek örnek garanti edilir. Double-checked locking: `if (null) { synchronized { if (null) { new ... } } }` — thread-safe ama her çağrıda `synchronized` maliyeti yok. Spring context'de: `@Bean @Scope("singleton")` ile Spring yapar — manuel Singleton gerekmez.

*Bu proje pedagojik amaçlıdır. Her satır Türkçe yorumlarla açıklanmıştır.*
