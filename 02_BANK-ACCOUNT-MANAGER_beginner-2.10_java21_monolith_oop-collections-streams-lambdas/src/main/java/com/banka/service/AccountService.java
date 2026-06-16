package com.banka.service;

import com.banka.enums.AccountType;
import com.banka.enums.TransactionType;
import com.banka.exception.AccountNotFoundException;
import com.banka.exception.InvalidAmountException;
import com.banka.model.*;
import com.banka.util.FileManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hesap işlemlerini yöneten servis sınıfı.
 *
 * ÖĞRENILEN KAVRAMLAR:
 *   - HashMap<K,V>: key-value çiftleri, hızlı arama
 *   - STREAM API: filter, map, collect, sorted, reduce
 *   - LAMBDA expressions: (param) -> işlem
 *   - GENERICS: <T> ile tip güvenli metodlar
 */
public class AccountService {

    // HashMap<String, Account>: hesap numarası → hesap nesnesi
    // Hızlı arama: O(1) karmaşıklık
    private final Map<String, Account> accounts; // HashMap implement eder Map'i

    // HashSet<String>: benzersiz hesap numaraları seti
    // Set: tekrar eden değer tutmaz
    private final Set<String> closedAccountNumbers;

    public AccountService() {
        this.accounts = new HashMap<>();
        this.closedAccountNumbers = new HashSet<>();
    }

    // ================================================================
    // HESAP OLUŞTURMA — Factory Method Pattern
    // ================================================================

    /**
     * Hesap türüne göre doğru alt sınıfı oluşturur.
     * POLYMORPHISM: dönen tip Account ama aslında alt sınıf
     */
    public Account createAccount(String customerId, AccountType type, double initialAmount) {
        if (initialAmount < 0) {
            throw new InvalidAmountException("Başlangıç bakiyesi negatif olamaz.");
        }

        // Switch-case — enum ile mükemmel uyum
        Account account = switch (type) {
            case SAVINGS  -> new SavingsAccount(customerId, initialAmount);
            case CHECKING -> new CheckingAccount(customerId, initialAmount);
            case CREDIT   -> new CreditAccount(customerId, initialAmount); // initialAmount = kredi limiti
        };

        // HashMap'e ekle: key=hesap numarası, value=hesap
        accounts.put(account.getAccountNumber(), account);

        FileManager.log("Hesap oluşturuldu: " + account.getAccountNumber() +
                        " | Tür: " + type.getDisplayName() +
                        " | Müşteri: " + customerId);

        return account;
    }

    // ================================================================
    // HESAP BULMA
    // ================================================================

    /**
     * Hesap numarasıyla hesap bul.
     * GENERICS örneği: <T extends Account> — Account veya alt sınıf döner
     */
    public Account findAccount(String accountNumber) {
        // HashMap.get(): O(1) arama
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /**
     * Müşteriye ait tüm hesapları getir.
     * STREAM API kullanımı:
     *   1. accounts.values() → tüm hesaplar
     *   2. .stream() → stream oluştur
     *   3. .filter() → koşula göre süz
     *   4. .collect() → sonuçları listeye topla
     */
    public List<Account> getAccountsByCustomer(String customerId) {
        return accounts.values().stream()
            // Lambda: her account için customerId eşleşiyor mu?
            .filter(account -> account.getCustomerId().equals(customerId))
            // Sonuçları ArrayList'e topla
            .collect(Collectors.toList());
    }

    /**
     * Türe göre hesap filtrele.
     * LAMBDA + STREAM örneği
     */
    public List<Account> getAccountsByType(AccountType type) {
        return accounts.values().stream()
            // Method reference veya lambda kullanılabilir
            .filter(acc -> acc.getAccountType() == type)
            // Bakiyeye göre büyükten küçüğe sırala
            .sorted((a1, a2) -> Double.compare(a2.getBalance(), a1.getBalance()))
            .collect(Collectors.toList());
    }

    // ================================================================
    // STREAM API — Gelişmiş Sorgular
    // ================================================================

    /**
     * Tüm hesapların toplam bakiyesi.
     * STREAM REDUCE: tüm değerleri tek değere indirgeme
     */
    public double getTotalBankBalance() {
        return accounts.values().stream()
            // map: her account'ı balance'a dönüştür (double stream)
            .mapToDouble(Account::getBalance) // method reference
            // sum(): toplam (reduce'un özel hali)
            .sum();
    }

    /**
     * En yüksek bakiyeli hesabı bul.
     * STREAM max() kullanımı
     */
    public Optional<Account> getHighestBalanceAccount() {
        return accounts.values().stream()
            // Comparator.comparingDouble: balance'a göre karşılaştır
            .max(Comparator.comparingDouble(Account::getBalance));
    }

    /**
     * Belirli miktarın üzerindeki hesapları filtrele.
     * LAMBDA + STREAM filter
     */
    public List<Account> getAccountsAboveBalance(double minBalance) {
        return accounts.values().stream()
            .filter(acc -> acc.getBalance() > minBalance) // Lambda ile filtrele
            .sorted(Comparator.comparingDouble(Account::getBalance).reversed())
            .collect(Collectors.toList());
    }

    /**
     * İşlem geçmişini filtrele — belirli türdeki işlemleri getir.
     * STREAM + FLATMAP: iç içe koleksiyonu düzleştirme
     */
    public List<TransactionRecord> getTransactionsByType(
            String accountNumber, TransactionType type) {

        Account account = findAccount(accountNumber);

        return account.getTransactionHistory().stream()
            // İşlem türüne göre filtrele
            .filter(tx -> tx.getType() == type)
            // Tarihe göre sırala (en yeni en üstte)
            .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
            .collect(Collectors.toList());
    }

    /**
     * Hesap istatistikleri — STREAM ile özet.
     */
    public void printAccountStats() {
        System.out.println("\n--- HESAP İSTATİSTİKLERİ ---");

        // Toplam hesap sayısı
        System.out.println("Toplam Hesap  : " + accounts.size());

        // Aktif hesap sayısı — filter + count
        long activeCount = accounts.values().stream()
            .filter(Account::isActive) // method reference
            .count(); // Eleman sayısını döner (long)
        System.out.println("Aktif Hesap   : " + activeCount);

        // Ortalama bakiye — mapToDouble + average
        OptionalDouble avgBalance = accounts.values().stream()
            .mapToDouble(Account::getBalance)
            .average(); // OptionalDouble döner (boş olabilir)
        // Optional: değer olabilir de olmayabilir de
        avgBalance.ifPresent(avg ->
            System.out.printf("Ortalama Bakiye: %.2f TL%n", avg)
        );

        // Tür bazında hesap sayıları — groupingBy
        Map<AccountType, Long> countByType = accounts.values().stream()
            .collect(Collectors.groupingBy(
                Account::getAccountType, // gruplandırma kriteri
                Collectors.counting()    // her grup için say
            ));

        System.out.println("Tür Dağılımı  :");
        // for-each ile Map entry'lerini gez (enhanced for loop)
        for (Map.Entry<AccountType, Long> entry : countByType.entrySet()) {
            System.out.printf("  %-25s: %d%n",
                entry.getKey().getDisplayName(), entry.getValue());
        }

        System.out.printf("Toplam Bakiye  : %.2f TL%n", getTotalBankBalance());
    }

    // ================================================================
    // HESAP KAPATMA / LİSTELEME
    // ================================================================

    public boolean closeAccount(String accountNumber) {
        Account account = findAccount(accountNumber);
        account.setActive(false);
        closedAccountNumbers.add(accountNumber); // HashSet'e ekle
        FileManager.log("Hesap kapatıldı: " + accountNumber);
        return true;
    }

    public List<Account> getAllAccounts() {
        // HashMap'deki tüm değerleri listeye çevir
        return new ArrayList<>(accounts.values());
    }

    public Map<String, Account> getAccountsMap() {
        return Collections.unmodifiableMap(accounts); // salt-okunur
    }
}
