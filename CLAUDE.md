# Java Eğitim — Claude Code Bağlam Dosyası

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

## Öğrenme Yöntemi

Tüm repolar baştan yazılmış durumda. Yaklaşım şu:

1. **Önce okuyarak anla** — Claude kavramı açıklar, mevcut kod üzerinden gösterir.
2. **Sonra kendin yaz** — Aynı kodu kapatıp sıfırdan yazmayı denersin.
3. **Pratik** — Her kavram için küçük egzersizler yapılır.

---

## Şu Anki Durum

**Aktif repo: 02 — BANK-ACCOUNT-MANAGER**

Tüm kod yazılmış. Kavramsal okuma aşamasındayız.

Tamamlanan kavramlar: Abstract Class, Interface vs Abstract Class, final field, static, Defensive Copy, protected, private static final, Runtime Polymorphism, Single Responsibility, Enum, Stream API, Lambda, HashMap/LinkedList/HashSet, Exception Handling, Optional, Composition, Generics, Wrapper/Autoboxing, Iterator, File I/O

Durum: **Kavramsal okuma tamamlandı.** Yazma pratiği sonraya bırakıldı.

---

## Şu Anki Durum

**Aktif repo: 01 — DSA-FUNDAMENTALS**

Tamamlanan konular: Big O, Arrays (Two Sum, Sliding Window, Two Pointer, Kadane, Prefix Sum)

Sıradaki: Hashing (Group Anagrams, LRU Cache)

---

## İşlenen Kavramlar — Tam Açıklamalarıyla

### 1. Abstract Class

**Neden var?**
Gerçek hayatta "Hesap" diye somut bir şey yok. Bankaya gittiğinde "tasarruf hesabı" ya da "vadesiz hesap" açtırıyorsun. `Account` soyut bir kavram — doğrudan örneklenemez.

```java
new Account(...)      // HATA — abstract sınıf örneklenemez
new SavingsAccount(…) // DOGRU
```

`abstract` keyword'ü tasarım niyetini koda gömer: "Bu sınıfı doğrudan kullanamazsın, alt sınıf yazman gerekiyor."

**Not defteri özeti:**
- `abstract class` doğrudan `new` ile örneklenemez
- Ortak davranışları (deposit, withdraw) bir kez yazar, alt sınıflar tekrar yazmaz
- `abstract` metodlar → alt sınıflar kendi implementasyonunu yazmak zorunda
- OOP'un Abstraction prensibini uygular
- Tasarım niyetini koda gömer — yanlış kullanımı derleme zamanında engeller

---

### 2. Interface vs Abstract Class

**Ayrım:**
- `abstract class` → "NE OLDUĞUNU" tanımlar (SavingsAccount IS-A Account)
- `interface` → "NE YAPABİLECEĞİNİ" tanımlar (Account CAN DO transactions)

**Neden interface daha esnek?**

```java
// transfer metodu Transactable alıyor, Account değil
public void transfer(Transactable targetAccount, double amount) {
    targetAccount.deposit(amount, "Transfer girişi");
}
```

Yarın `DigitalWallet` diye bambaşka bir sınıf yazsan ve `Transactable` implement etsen — bu transfer metodu ona da para gönderebilir. `Account`'u extend etmesine gerek yok. Java'da bir sınıf sadece 1 class extend edebilir ama birden fazla interface implement edebilir.

**Not defteri özeti:**
- `abstract class` = "ne olduğu" ilişkisi (IS-A)
- `interface` = "yetenek" ilişkisi (CAN-DO)
- Bir sınıf → 1 class extend eder, N interface implement eder
- Interface parametresi → kalıtım hiyerarşisinden bağımsız esneklik
- `default` metod ile interface'e implementasyon da eklenebilir (Java 8+)

---

### 3. final Field + Constructor İlişkisi

**Neden final?**
`TransactionRecord`'daki tüm alanlar `final` — çünkü banka işlemi geçmişe dönük değiştirilemez. 500 TL çektin, kayıt oluştu. O kaydın miktarı sonradan değişebilseydi bu sahtekarlık olurdu.

**Constructor ile bağlantı:**
`final` = "bir kez ata, sonra dokunma." Değer dışarıdan gelecekse (her işlemde farklı) satır üzerinde yazamazsın. Tek atama şansın constructor'dır.

```java
private final double amount;  // değer henüz bilinmiyor

public TransactionRecord(double amount) {
    this.amount = amount;  // tek seferlik atama — bitti
}
// Constructor sonrası this.amount = x; → derleme hatası
```

**Not defteri özeti:**
- `final` = bir kez ata, sonra değiştirme
- Değer dışarıdan gelecekse → constructor'da atarsın
- Constructor = nesne doğduğu anda çalışan tek seferlik blok
- Constructor bittikten sonra `final` field'a dokunulamazsın
- Immutable nesne = veri bütünlüğü garantisi (banka kaydı örneği)

---

### 4. static — Sınıfa Ait Tek Kopya

**Problem:** `transactionId` benzersiz olmalı. Her nesne kendi sayacını tutsa hepsi 1'den başlar → aynı ID, sistem çöker.

**Çözüm:** `static` sayaç → sınıfa ait tek kopya, tüm nesneler paylaşır.

```
                ┌─────────────────────────────┐
                │   TransactionRecord  SINIFI  │
                │   transactionCounter = 3     │  ← tek kopya
                └─────────────────────────────┘
                     ↑          ↑         ↑
                  işlem1     işlem2    işlem3
                  (id=1)     (id=2)    (id=3)
```

```java
this.transactionId = ++transactionCounter;
// ++x → önce artır sonra al (x++ → önce al sonra artır)
```

**Not defteri özeti:**
- `static` değişken → nesneye değil sınıfa aittir
- Bellekte tek kopya — tüm nesneler paylaşır
- `static` olmadan her nesne kendi kopyasını başlatır → benzersiz ID imkansız
- `++x` önce artır sonra al, `x++` önce al sonra artır
- `private static` → sınıf içi ortak veri, dışarıdan erişim yok

---

### 5. Defensive Copy

**Problem:** Java'da nesne değişkenleri referans tutar — nesnenin bellekteki adresini.

```java
return transactionHistory;  // dışarıya adresi verirsin
```

Dışarıdaki kod `history.clear()` dese orijinal liste de silinir — aynı adresi gösteriyorlar.

**Çözüm:**
```java
return new ArrayList<>(transactionHistory);  // yeni liste, yeni adres
```

İki ayrı liste. Dışarıdaki kopyaya ne yapılırsa yapılsın orijinale dokunamaz.

**Not defteri özeti:**
- Java'da nesneler referansla tutulur — değer değil bellek adresi
- `return list` → dışarıya orijinalin adresini verirsin
- Dışarıdaki kod değiştirirse orijinal de bozulur
- `return new ArrayList<>(list)` → yeni liste, yeni adres, orijinal korunur
- Bu teknik: Defensive Copy (savunmacı kopya)
- `private` tek başına yetmez — referans sızdırmamak da encapsulation'ın parçası

---

### 6. protected vs public — En Az Yetki Prensibi

**Erişim seviyeleri (kısıtlıdan gevşeğe):**
```
private   → sadece bu sınıf
protected → bu sınıf + alt sınıflar + aynı paket
public    → herkes
```

`balance` setter'ı `public` olsaydı:
```java
account.setBalance(999999.0);  // işlem kaydı yok, validasyon yok
```

`deposit()` ve `withdraw()`'un tüm kontrolleri bypass edilir. `protected` diyerek: "Bakiyeyi sadece bu sınıf ve çocukları değiştirebilir" diyorsun.

**Not defteri özeti:**
- En az yetki prensibi: erişimi her zaman ihtiyaç kadar aç, fazlasını değil
- `protected` = alt sınıflara kontrollü güven
- `public` setter → validasyon/kayıt olmadan değiştirme kapısı açar
- Erişim ne kadar genişse yanlış kullanım o kadar kolay

---

### 7. private static final — Sınıf Sabiti

Üç keyword'ün kombinasyonu Java'da standart bir kalıptır.

```java
private static final double MINIMUM_BALANCE = 100.0;
```

- `private` → dışarıya kapalı
- `static` → tek kopya, nesneye değil sınıfa bağlı (1000 hesap = 1 kopya)
- `final` → değer bir kez atanır, değiştirilemez

İsimlendirme: `BÜYÜK_HARF_VE_ALT_ÇIZGI` (Java convention)

**Not defteri özeti:**
- `private static final` = Java sınıf sabiti kalıbı
- İsimlendirme: `BÜYÜK_HARF_VE_ALT_ÇIZGI`
- `static` → 1000 nesne olsa bile bellekte tek kopya
- `final` → runtime'da değiştirilemez
- Kullanım: iş kuralları, sabit değerler, magic number'ları isimlendirmek

---

### 8. Runtime Polymorphism / Dynamic Dispatch

**Nasıl çalışır:**
```java
// Account.java
public void withdraw(double amount, String description) {
    validateAmount(amount);
    checkWithdrawEligibility(amount);  // hangi versiyon çalışacak?
    this.balance -= amount;
}
```

`withdraw()` compile time'da yazılmış. Ama `checkWithdrawEligibility` hangi nesne üzerinde çalıştığına runtime'da karar verir.

```java
Account hesap = new SavingsAccount(...);
hesap.withdraw(500, "Market");
// → SavingsAccount'un checkWithdrawEligibility'si çalışır
```

| Hesap türü | Çalışan kural |
|-----------|---------------|
| SavingsAccount | min bakiye + günlük limit |
| CheckingAccount | overdraft limiti |
| CreditAccount | kredi limiti |

`withdraw()` tek satır — 3 farklı kural. Yarın yeni hesap türü eklenirse `withdraw()` koduna dokunulmaz.

**Not defteri özeti:**
- Runtime polymorphism = hangi metod çalışacağı runtime'da belirlenir
- Değişken tipi `Account` olsa bile içindeki nesnenin metodunu çağırır
- Buna Dynamic Dispatch denir
- `abstract` metod → "bu metod var ama nasıl çalıştığını sen yaz" sözleşmesi
- `@Override` → derleyiciye "üst sınıfı override ediyorum" garantisi
- Yeni alt sınıf yazınca mevcut `withdraw()` değişmez → Open/Closed Prensibi

---

### 9. Single Responsibility Prensibi

`instanceof` ile tip kontrolü kötü tasarımdır:
```java
// YANLIŞ
if (this instanceof SavingsAccount) { ... }
else if (this instanceof CheckingAccount) { ... }
```

Her yeni hesap türünde bu metoda girip `else if` eklemek gerekir. 10 tür olsa 100 satır olur.

Doğru yaklaşım: her sınıf kendi kuralını kendi içinde taşır. `withdraw()` kimsenin işine karışmaz, sadece `checkWithdrawEligibility()` çağırır.

**Not defteri özeti:**
- Her sınıf tek bir şeyden sorumlu olmalı
- `instanceof` ile tip kontrolü = her yeni türde mevcut kodu değiştirmek zorunda
- Doğrusu: her sınıf kendi davranışını kendi içinde taşır
- Open/Closed: mevcut kodu değiştirme, yeni sınıf ekle

---

### 10. Enum

**Problem:** Hesap türünü `int` veya `String` ile temsil etmek:
```java
account.setAccountType(1);        // 1 ne demek?
account.setAccountType("savigns"); // yazım hatası — derleyici görmez
account.setAccountType(9);        // geçerli mi? Kimse bilmez.
```

**Çözüm:** Enum → sadece tanımlı değerler alınabilir, yanlış değer derlenmez.

Enum aslında bir class — field, constructor, metod taşıyabilir:
```java
SAVINGS("Tasarruf Hesabı", 0.05)
// → displayName = "Tasarruf Hesabı", interestRate = 0.05
```

Bu sayede hesap türünü soran kişi hem adını hem faiz oranını aynı nesneden alır. `int` kullansaydın bu bilgiyi başka bir yerde `if/else` ile üretirdin.

**Not defteri özeti:**
- Enum = sabit değer kümesi — sadece tanımlı değerler alınabilir
- `int` veya `String` yerine enum → tip güvenliği (yanlış değer derlenmez)
- Enum aslında bir class — field, constructor, metod olabilir
- Her enum sabiti o constructor'ı çağırır: `SAVINGS("...", 0.05)`
- `values()` → tüm sabitler dizi olarak gelir
- `toString()` override → yazdırıldığında anlamlı çıktı

---

## Önemli Kavramlar Sözlüğü

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
| Open/Closed Prensibi | Mevcut kodu değiştirme, yeni sınıf ekle |
| Enum | Sabit değer kümesi, aslında class, field ve metod taşıyabilir |
| Least Privilege | Erişimi her zaman ihtiyaç kadar aç, fazlasını değil |
| Stream API | Koleksiyon üzerinde filter/map/collect zinciri — kaynak → ara op → terminal |
| Lambda | `param -> ifade` — anonim fonksiyon, tek satır |
| Method Reference | `Sınıf::metod` — `x -> x.metod()` ile aynı, daha kısa |
| Optional<T> | Değer olabilir de olmayabilir de — null yerine tip güvenli sarmalayıcı |
| HashMap | Anahtar-değer, O(1) arama — "numarayla anında bul" |
| LinkedList | Bağlı düğümler, O(1) ekleme/silme, ortadan silme hızlı |
| HashSet | Tekrarsız küme, O(1) "var mı?" kontrolü |
| Checked / Unchecked Exception | Checked zorunlu handle; RuntimeException isteğe bağlı |
| Custom Exception | `extends RuntimeException`, `super(mesaj)`, yapısal veri taşıyabilir |
| try-with-resources | Blok bitince `close()` garantili — kaynak sızıntısı yok |
| Composition (HAS-A) | Sınıf başka sınıfı field olarak tutar, kalıtım yerine tercih edilir |
| Generics `<T>` | Tip parametresi — yanlış tip derleme zamanında yakalanır, cast gerekmez |
| Autoboxing / Unboxing | primitive ↔ wrapper otomatik dönüşüm; karşılaştırmada `.equals()` kullan |
| Iterator | `hasNext()` + `next()` — gezarken güvenli silme için `iterator.remove()` |
| BufferedWriter | Tampona yaz, toplu diske gönder — `FileWriter`'dan performanslı |

---

## 01 — DSA-FUNDAMENTALS Kavramları

### Big O Notasyonu

Girdi büyüdükçe işlem sayısının nasıl büyüdüğünü ölçer — makine bağımsız.

```
O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2ⁿ)
```

Kurallar:
- Sabitleri at: O(3n) → O(n)
- Baskın terimi al: O(n² + n) → O(n²)
- Space complexity: hafıza kullanımı için aynı notasyon

Array Big O: Erişim O(1) | Arama O(n) | Ekleme/Silme O(n) — shift gerekir

---

### Array Teknikleri

**Two Sum — HashMap ile O(n²) → O(n)**

"Tamamlayan sayıyı daha önce gördüm mü?" sorusunu HashMap'e sor.

```java
int complement = target - nums[i];
if (map.containsKey(complement)) return cevap;
map.put(nums[i], i);
```

Kalıp: "İki eleman arasında ilişki ara" → complement = target - nums[i] → HashMap

---

**Sliding Window — Pencereyi kaydır**

Sabit k boyutunda alt dizi: Her adımda giren ekle, çıkan çıkar. O(n).

```java
windowSum += nums[i] - nums[i - k]; // giren - çıkan
```

Kalıp: "Belirli uzunlukta alt dizi toplamı/maksimumu" → Sliding Window

---

**Two Pointer — İki uçtan sıkıştır**

Sıralı dizide iki eleman arasında ilişki. Sol + sağ, toplam hedeften küçükse sol ileri, büyükse sağ geri.

```java
if (sum < target) left++;
else              right--;
```

Kalıp: Sıralı dizide iki eleman ilişkisi, palindrome, trapping rain water

---

**Kadane — Max Subarray**

Her noktada "devam et mi, baştan başla mı?" kararı. O(n).

```java
currentSum = Math.max(nums[i], currentSum + nums[i]);
maxSum = Math.max(maxSum, currentSum);
```

currentSum negatifse taşıma, bırak — yeniden başla.

---

**Prefix Sum — Sorguları O(1)'e indir**

Ön işleme O(n), ardından her aralık sorgusu O(1).

```java
prefix[i+1] = prefix[i] + nums[i];           // ön işleme
rangeSum = prefix[right+1] - prefix[left];    // sorgu
```

Kalıp: Aynı dizi üzerinde çok sayıda aralık toplamı sorgusu

