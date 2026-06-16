package com.banka.model;

import com.banka.enums.AccountType;
import com.banka.enums.TransactionType;
import com.banka.exception.InsufficientFundsException;
import com.banka.exception.InvalidAmountException;
import com.banka.interfaces.Printable;
import com.banka.interfaces.Transactable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ABSTRACT CLASS (Soyut Sınıf) KAVRAMI:
 *
 * abstract keyword'ü ile işaretlenmiş sınıf doğrudan oluşturulamaz:
 *   new Account(...) → HATA!
 *
 * Neden kullanırız?
 *   - Ortak davranışları (deposit, withdraw) bir kez yazar,
 *     alt sınıflar tekrar yazmak zorunda kalmaz.
 *   - abstract metodlar → alt sınıflar KENDİ implementasyonunu YAZMAK ZORUNDA.
 *   - OOP'un "Abstraction" prensibini uygular.
 *
 * Bu sınıf hem Transactable (para işlemleri) hem de
 * Printable (yazdırma) interface'lerini implement eder.
 */
public abstract class Account implements Transactable, Printable {

    // ---- ENCAPSULATION: tüm alanlar private ----
    // Dışarıdan doğrudan erişim yok, sadece getter/setter ile

    private final String accountNumber;      // Hesap numarası (değişmez)
    private final String customerId;         // Hesap sahibinin ID'si
    private final AccountType accountType;   // Hesap türü (enum)
    private double balance;                  // Bakiye (değişebilir)
    private final LocalDateTime openedDate;  // Hesap açılış tarihi (değişmez)
    private boolean active;                  // Hesap aktif mi?

    // GENERICS örneği: List<TransactionRecord> — sadece TransactionRecord tutar
    // ArrayList: dinamik büyüyen dizi (Collections framework)
    private final List<TransactionRecord> transactionHistory;

    // static: sınıfa ait — her hesap için ortak sayaç
    private static int accountCounter = 1000; // Hesap numaraları 1000'den başlar

    // protected constructor: sadece alt sınıflar çağırabilir (access modifier)
    protected Account(String customerId, AccountType accountType, double initialBalance) {
        // static sayacı artır, benzersiz hesap numarası oluştur
        this.accountNumber = "ACC" + String.format("%06d", ++accountCounter);
        this.customerId = customerId;
        this.accountType = accountType;
        this.balance = initialBalance;
        this.openedDate = LocalDateTime.now();
        this.active = true; // Yeni hesap aktif açılır
        // ArrayList: boş liste, işlemler eklendikçe büyür
        this.transactionHistory = new ArrayList<>();

        // Açılış işlemini kaydet (eğer başlangıç bakiyesi varsa)
        if (initialBalance > 0) {
            TransactionRecord openingRecord = new TransactionRecord(
                this.accountNumber,
                TransactionType.DEPOSIT,
                initialBalance,
                initialBalance,
                "Hesap açılış bakiyesi"
            );
            transactionHistory.add(openingRecord);
        }
    }

    // ================================================================
    // TRANSACTABLE INTERFACE İMPLEMENTASYONU
    // ================================================================

    /**
     * Para yatırma işlemi.
     * EXCEPTION HANDLING: geçersiz miktar kontrolü
     */
    @Override
    public void deposit(double amount, String description) {
        // Miktar kontrolü — InvalidAmountException fırlat
        validateAmount(amount);

        // Bakiyeyi artır
        double previousBalance = this.balance;
        this.balance += amount; // += kısaltması: balance = balance + amount

        // İşlemi geçmişe kaydet
        TransactionRecord record = new TransactionRecord(
            accountNumber, TransactionType.DEPOSIT, amount, balance, description
        );
        transactionHistory.add(record);

        System.out.printf("✓ Para yatırıldı: %.2f TL | Yeni bakiye: %.2f TL%n", amount, balance);
    }

    /**
     * Para çekme işlemi — abstract değil, ortak mantık burada.
     * Alt sınıflar ek kural eklemek isterse override edebilir.
     */
    @Override
    public void withdraw(double amount, String description) {
        // Miktar geçerli mi?
        validateAmount(amount);
        // Yeterli bakiye var mı? (abstract metod çağırıyoruz!)
        checkWithdrawEligibility(amount);

        // Bakiyeden düş
        this.balance -= amount;

        // İşlemi kaydet
        TransactionRecord record = new TransactionRecord(
            accountNumber, TransactionType.WITHDRAWAL, amount, balance, description
        );
        transactionHistory.add(record);

        System.out.printf("✓ Para çekildi: %.2f TL | Yeni bakiye: %.2f TL%n", amount, balance);
    }

    /**
     * Hesaplar arası transfer.
     * Polymorphism: hedef hesap Transactable olduğu sürece
     * hangi ALT TÜR olduğu önemli değil!
     */
    @Override
    public void transfer(Transactable targetAccount, double amount) {
        validateAmount(amount);
        checkWithdrawEligibility(amount);

        // Bu hesaptan çek
        this.balance -= amount;
        TransactionRecord outRecord = new TransactionRecord(
            accountNumber, TransactionType.TRANSFER, amount, balance,
            "Transfer çıkışı"
        );
        transactionHistory.add(outRecord);

        // Hedef hesaba yatır (interface üzerinden çağırıyoruz — polymorphism!)
        targetAccount.deposit(amount, "Transfer girişi: " + accountNumber);

        System.out.printf("✓ Transfer tamamlandı: %.2f TL%n", amount);
    }

    /**
     * ABSTRACT METOD: alt sınıflar KENDİ kurallarına göre yazacak.
     * SavingsAccount farklı, CreditAccount farklı davranır.
     */
    protected abstract void checkWithdrawEligibility(double amount);

    /**
     * Faiz hesaplama: her alt sınıfın faiz mantığı farklı
     */
    public abstract double calculateInterest();

    // ================================================================
    // PRINTABLE INTERFACE İMPLEMENTASYONU
    // ================================================================

    @Override
    public void printSummary() {
        System.out.printf("%-12s | %-25s | %12.2f TL | %s%n",
            accountNumber, accountType.getDisplayName(), balance,
            active ? "Aktif" : "Pasif"
        );
    }

    @Override
    public void printDetails() {
        printSeparator(); // Printable interface'in default metodu
        System.out.println("HESAP DETAYLARI");
        System.out.println("Hesap No    : " + accountNumber);
        System.out.println("Hesap Türü  : " + accountType.getDisplayName());
        System.out.printf ("Bakiye      : %.2f TL%n", balance);
        System.out.println("Durum       : " + (active ? "Aktif" : "Pasif"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        System.out.println("Açılış Tarihi: " + openedDate.format(fmt));
        System.out.println("İşlem Sayısı : " + transactionHistory.size());
        printSeparator();
    }

    // ================================================================
    // ITERATOR PATTERN — işlem geçmişini dolaşmak için
    // ================================================================

    /**
     * ITERATOR PATTERN: Koleksiyonu dolaşmak için standart yol.
     * ArrayList.iterator() → bir Iterator<TransactionRecord> döner.
     * hasNext() + next() ile sırayla erişim.
     */
    public void printTransactionHistory() {
        printSeparator();
        System.out.println("İŞLEM GEÇMİŞİ: " + accountNumber);
        printSeparator();

        if (transactionHistory.isEmpty()) {
            System.out.println("  Henüz işlem yok.");
        } else {
            // Iterator pattern kullanımı
            Iterator<TransactionRecord> iterator = transactionHistory.iterator();
            while (iterator.hasNext()) { // Sonraki eleman var mı?
                TransactionRecord record = iterator.next(); // Sonraki elemanı al
                System.out.println("  " + record);
            }
        }
        printSeparator();
    }

    // ================================================================
    // YARDIMCI METODLAR
    // ================================================================

    // Miktarın geçerli (pozitif) olup olmadığını kontrol eder
    private void validateAmount(double amount) {
        // Autoboxing: double → Double (wrapper class)
        Double boxedAmount = amount; // primitive → wrapper (autoboxing)
        // Unboxing: Double → double (otomatik)
        if (boxedAmount <= 0.0) {
            throw new InvalidAmountException(amount);
        }
    }

    // İşlem geçmişine erişim (dışarıdan değiştirilemesin diye kopya döner)
    public List<TransactionRecord> getTransactionHistory() {
        // Yeni liste oluştur: orijinal listeyi koru (defensive copy)
        return new ArrayList<>(transactionHistory);
    }

    // ================================================================
    // GETTER / SETTER METODLARI (Encapsulation)
    // ================================================================

    public String getAccountNumber() { return accountNumber; }
    public String getCustomerId() { return customerId; }
    public AccountType getAccountType() { return accountType; }
    public boolean isActive() { return active; }
    public LocalDateTime getOpenedDate() { return openedDate; }

    // Getter — balance dışarıdan okunabilir ama direkt yazılamaz
    @Override
    public double getBalance() { return balance; }

    // protected setter — sadece bu paketteki sınıflar ve alt sınıflar değiştirebilir
    protected void setBalance(double balance) { this.balance = balance; }

    // Hesabı aktif/pasif yap
    public void setActive(boolean active) { this.active = active; }
}
