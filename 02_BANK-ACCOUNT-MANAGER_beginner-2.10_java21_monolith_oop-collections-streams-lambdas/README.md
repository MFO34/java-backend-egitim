# 🏦 Banka Hesap Yönetim Sistemi

> Core Java öğrenmek için geliştirilmiş kapsamlı konsol uygulaması.
> Her Java konusu gerçek bir banka senaryosuyla öğretilmiştir.

---

## 📋 Proje Açıklaması

Bu proje, **Core Java** kavramlarının tamamını tek bir uygulamada göstermek amacıyla geliştirilmiştir. Saf Java (framework yok, kütüphane yok) kullanılarak yazılmıştır. Her dosya bir veya birkaç Java konusunu odağa alır ve Türkçe açıklayıcı yorumlarla desteklenir.

---

## 🎓 Öğrenilen Core Java Konuları

| Java Konusu | Bulunduğu Dosya | Açıklama |
|---|---|---|
| Değişkenler, veri tipleri | `TransactionRecord.java` | `int`, `double`, `String`, `boolean`, `long` |
| Koşullar (if/else) | `SavingsAccount.java`, `BankApp.java` | Bakiye kontrolleri, menü koşulları |
| Switch-case | `AccountService.java`, `BankApp.java` | Hesap türü seçimi, menü yönetimi |
| Döngüler (for, while, do-while) | `BankApp.java`, `Account.java` | Menü döngüsü, işlem listesi, demo veri |
| Diziler | `BankApp.java` | `double[] depositAmounts`, `String[] descriptions` |
| ArrayList | `Account.java`, `Customer.java` | İşlem geçmişi, hesap listesi |
| HashMap | `AccountService.java` | Hesap numarası → hesap eşleşmesi |
| HashSet | `AccountService.java` | Kapalı hesap numaraları kümesi |
| LinkedList | `BankService.java` | Müşteri listesi |
| Generics (`<T>`) | `AccountService.java`, `BankService.java` | `List<Account>`, `Map<String, Account>` |
| Encapsulation | `Account.java`, `Customer.java` | `private` alan + getter/setter |
| Inheritance (extends) | `SavingsAccount`, `CheckingAccount`, `CreditAccount` | `extends Account`, `super()` |
| Polymorphism | `BankService.java`, `AccountService.java` | Method overriding, `Account` tipiyle alt sınıf kullanımı |
| Abstraction (abstract class) | `Account.java` | `abstract` class, `abstract` metodlar |
| Interface | `Transactable.java`, `Printable.java` | Çoklu interface implementation |
| Exception Handling | `InsufficientFundsException.java` vb. | `try/catch/finally`, custom exception |
| String manipülasyonu | `Customer.java`, `TransactionRecord.java` | `StringBuilder`, `String.format`, `contains`, `trim` |
| Lambda expressions | `BankService.java`, `AccountService.java` | `filter(acc -> ...)`, `forEach(...)` |
| Stream API | `AccountService.java`, `BankService.java` | `filter`, `map`, `collect`, `sorted`, `reduce` |
| Enum | `AccountType.java`, `TransactionType.java` | Sabit değer kümeleri |
| Static keyword | `FileManager.java`, `Account.java` | Static sayaç, static metodlar |
| Final keyword | `TransactionRecord.java` | Değiştirilemez alanlar |
| Access modifiers | Tüm dosyalar | `public`, `private`, `protected` kullanımı |
| Wrapper classes | `Customer.java` | `Integer`, `Double` autoboxing/unboxing |
| File I/O | `FileManager.java` | `BufferedWriter`, `BufferedReader`, `FileWriter` |
| Iterator pattern | `Account.java` | `Iterator<TransactionRecord>` ile `while` |

---

## 📁 Proje Yapısı

```
banka-hesap-yonetim/
│
├── src/main/java/com/banka/
│   │
│   ├── app/
│   │   └── BankApp.java          → Ana sınıf (main metodu, konsol menüsü)
│   │
│   ├── model/                    → Veri modelleri (OOP sınıfları)
│   │   ├── Account.java          → Abstract temel hesap sınıfı
│   │   ├── SavingsAccount.java   → Tasarruf hesabı (extends Account)
│   │   ├── CheckingAccount.java  → Vadesiz hesap (extends Account)
│   │   ├── CreditAccount.java    → Kredi hesabı (extends Account)
│   │   ├── Customer.java         → Müşteri sınıfı
│   │   └── TransactionRecord.java → İşlem kaydı (immutable)
│   │
│   ├── service/                  → İş mantığı katmanı
│   │   ├── AccountService.java   → Hesap CRUD + Stream API sorguları
│   │   └── BankService.java      → Müşteri yönetimi + hesap işlemleri
│   │
│   ├── exception/                → Özel hata sınıfları
│   │   ├── InsufficientFundsException.java  → Yetersiz bakiye
│   │   ├── AccountNotFoundException.java    → Hesap bulunamadı
│   │   └── InvalidAmountException.java      → Geçersiz miktar
│   │
│   ├── util/                     → Yardımcı araçlar
│   │   ├── FileManager.java      → Dosya okuma/yazma (File I/O)
│   │   └── ConsoleHelper.java    → Konsol giriş/çıkış yardımcısı
│   │
│   ├── enums/                    → Sabit değer kümeleri
│   │   ├── AccountType.java      → SAVINGS, CHECKING, CREDIT
│   │   └── TransactionType.java  → DEPOSIT, WITHDRAWAL, TRANSFER
│   │
│   └── interfaces/               → Sözleşme tanımları
│       ├── Transactable.java     → Para işlemleri arayüzü
│       └── Printable.java        → Yazdırma arayüzü
│
├── data/                         → Runtime'da oluşur (gitignore'da)
│   ├── islem_gecmisi.txt         → İşlem geçmişi dosyası
│   └── banka_log.txt             → Sistem log dosyası
│
├── pom.xml                       → Maven yapılandırması
├── .gitignore                    → Git dışlama listesi
└── README.md                     → Bu dosya
```

---

## ⚙️ Kurulum ve Çalıştırma

### Gereksinimler
- Java JDK 17 veya üzeri
- Apache Maven 3.6+
- Git

### Adım 1: Projeyi Klonla
```bash
git clone https://github.com/m-ogur34/banka-hesap-yonetim.git
cd banka-hesap-yonetim
```

### Adım 2: Derle
```bash
mvn compile
```

### Adım 3: Çalıştır
```bash
mvn exec:java -Dexec.mainClass="com.banka.app.BankApp"
```

### Alternatif: JAR oluştur ve çalıştır
```bash
mvn package
java -jar target/banka-hesap-yonetim.jar
```

---

## 🖥️ Uygulama Ekranları (ASCII)

```
========================================================
              CLAUDE BANK A.Ş. - ANA MENÜ
========================================================
  1. Müşteri İşlemleri
  2. Hesap İşlemleri
  3. Para İşlemleri
  4. Sorgulama ve Raporlar
  5. Sistem İşlemleri
  0. Çıkış
========================================================
Seçiminiz: _
```

```
========================================================
                   PARA İŞLEMLERİ
========================================================
  1. Para Yatır
  2. Para Çek
  3. Hesaplar Arası Transfer
  4. İşlem Geçmişi Görüntüle
  0. Ana Menüye Dön
--------------------------------------------------
Seçiminiz: 1
Hesap No   : ACC001001
Miktar (TL): 1000
Açıklama   : Maaş
✓ Para yatırıldı: 1000.00 TL | Yeni bakiye: 6000.00 TL
```

```
İŞLEM GEÇMİŞİ: ACC001001
--------------------------------------------------
  [#0001] 23.04.2026 14:30 | + 5000.00 TL | Bakiye: 5000.00 TL | Para Yatırma | Hesap açılış
  [#0004] 23.04.2026 14:30 | + 500.00 TL  | Bakiye: 5500.00 TL | Para Yatırma | Maaş
  [#0005] 23.04.2026 14:30 | + 1000.00 TL | Bakiye: 6500.00 TL | Para Yatırma | Kira geliri
  [#0007] 23.04.2026 14:30 | - 300.00 TL  | Bakiye: 6200.00 TL | Para Çekme   | Market alışverişi
--------------------------------------------------
```

---

## 🏗️ OOP Prensipleri ve Örnekleri

### 1. Encapsulation (Kapsülleme)
```java
// Account.java — private alan, public getter
private double balance;
public double getBalance() { return balance; }
```

### 2. Inheritance (Kalıtım)
```java
// SavingsAccount.java
public class SavingsAccount extends Account {
    public SavingsAccount(String customerId, double initialBalance) {
        super(customerId, AccountType.SAVINGS, initialBalance); // üst sınıf
    }
}
```

### 3. Polymorphism (Çok Biçimlilik)
```java
// Tüm hesap türleri Account tipinde tutulabilir
List<Account> accounts = new ArrayList<>();
accounts.add(new SavingsAccount(...));   // Tasarruf
accounts.add(new CheckingAccount(...));  // Vadesiz
accounts.add(new CreditAccount(...));    // Kredi

// Her biri kendi withdraw() kuralını çalıştırır!
for (Account acc : accounts) {
    acc.withdraw(100, "test"); // Polymorphism
}
```

### 4. Abstraction (Soyutlama)
```java
// Account.java — abstract class
public abstract class Account {
    protected abstract void checkWithdrawEligibility(double amount);
    public abstract double calculateInterest();
}
```

---

## 📖 Örnek Kullanım Senaryoları

**Senaryo 1: Yeni müşteri ve hesap açma**
1. Ana Menü → 1 (Müşteri İşlemleri) → 1 (Yeni Müşteri Ekle)
2. Ad, Soyad, E-posta, Telefon, TC gir
3. Ana Menü → 2 (Hesap İşlemleri) → 1 (Yeni Hesap Aç)
4. Müşteri ID'sini gir, tür seç, bakiye gir

**Senaryo 2: Transfer işlemi**
1. Ana Menü → 3 (Para İşlemleri) → 3 (Transfer)
2. Kaynak hesap no → Hedef hesap no → Miktar gir

**Senaryo 3: İşlem geçmişini kaydet**
1. Ana Menü → 5 (Sistem) → 1 (İşlem Geçmişini Kaydet)
2. `data/islem_gecmisi.txt` dosyasını incele

---

## 🛠️ Teknolojiler

| Teknoloji | Versiyon | Amaç |
|---|---|---|
| Java (JDK) | 17+ | Ana programlama dili |
| Apache Maven | 3.6+ | Proje yönetimi ve derleme |
| Git | - | Versiyon kontrolü |

---

*Bu proje kodlama öğrenmek amacıyla geliştirilmiştir. Her satır Türkçe yorumlarla açıklanmıştır.*
