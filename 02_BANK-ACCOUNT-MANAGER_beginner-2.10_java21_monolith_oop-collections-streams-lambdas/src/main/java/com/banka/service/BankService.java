package com.banka.service;

import com.banka.enums.AccountType;
import com.banka.exception.AccountNotFoundException;
import com.banka.model.*;
import com.banka.util.FileManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ana banka servisi — müşteri ve hesap yönetimini birleştirir.
 *
 * ÖĞRENILEN KAVRAMLAR:
 *   - LinkedList: çift bağlantılı liste — silme/ekleme O(1)
 *   - Collections.sort: listeyi sırala
 *   - Comparator: özel sıralama kriteri
 */
public class BankService {

    // LinkedList: müşteri listesi için
    // ArrayList vs LinkedList: ArrayList arama için hızlı, LinkedList ekleme/silme için
    private final LinkedList<Customer> customers;

    // AccountService'e delege et (Composition pattern)
    private final AccountService accountService;

    // Banka adı — final: değişmez
    private final String bankName;

    public BankService(String bankName) {
        this.bankName = bankName;
        // LinkedList oluşturma
        this.customers = new LinkedList<>();
        this.accountService = new AccountService();
        FileManager.initializeDataDirectory();
        FileManager.log("Banka sistemi başlatıldı: " + bankName);
    }

    // ================================================================
    // MÜŞTERİ YÖNETİMİ
    // ================================================================

    public Customer addCustomer(String firstName, String lastName,
                                 String email, String phone, String nationalId) {
        // TC Kimlik benzersizlik kontrolü
        boolean exists = customers.stream()
            .anyMatch(c -> c.getNationalId().equals(nationalId));

        if (exists) {
            throw new IllegalArgumentException(
                "Bu TC Kimlik numarası zaten kayıtlı: " + nationalId);
        }

        Customer customer = new Customer(firstName, lastName, email, phone, nationalId);
        customers.add(customer); // LinkedList'e ekle

        FileManager.log("Müşteri eklendi: " + customer.getCustomerId() +
                        " | " + customer.getFullName());

        System.out.println("Müşteri oluşturuldu: " + customer.getCustomerId() +
                           " - " + customer.getFullName());
        return customer;
    }

    /**
     * Müşteriyi ID ile bul.
     * STREAM filter + findFirst
     */
    public Customer findCustomer(String customerId) {
        // Optional<Customer>: müşteri olabilir de olmayabilir
        Optional<Customer> found = customers.stream()
            .filter(c -> c.getCustomerId().equals(customerId))
            .findFirst(); // İlk eşleşeni döner

        // orElseThrow: Optional boşsa exception fırlat
        return found.orElseThrow(() ->
            new AccountNotFoundException("Müşteri bulunamadı: " + customerId)
        );
    }

    /**
     * İsme göre müşteri ara.
     * String.toLowerCase() ile büyük/küçük harf duyarsız arama.
     */
    public List<Customer> searchCustomers(String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        return customers.stream()
            // Ad veya soyad içeriyor mu?
            .filter(c ->
                c.getFirstName().toLowerCase().contains(lowerKeyword) ||
                c.getLastName().toLowerCase().contains(lowerKeyword) ||
                c.getEmail().toLowerCase().contains(lowerKeyword)
            )
            .collect(Collectors.toList());
    }

    /**
     * Müşteri sil.
     */
    public boolean removeCustomer(String customerId) {
        Customer customer = findCustomer(customerId);

        // Hesapları kapat
        for (Account account : customer.getAccounts()) {
            account.setActive(false);
        }

        // LinkedList.removeIf: koşulu sağlayan elemanları sil
        boolean removed = customers.removeIf(c -> c.getCustomerId().equals(customerId));

        if (removed) {
            FileManager.log("Müşteri silindi: " + customerId);
            System.out.println("Müşteri silindi: " + customerId);
        }
        return removed;
    }

    // ================================================================
    // HESAP İŞLEMLERİ — AccountService'e delege et
    // ================================================================

    /**
     * Müşteriye hesap aç.
     */
    public Account openAccount(String customerId, AccountType type, double initialAmount) {
        Customer customer = findCustomer(customerId);

        // AccountService üzerinden hesap oluştur
        Account account = accountService.createAccount(customerId, type, initialAmount);

        // Müşteri nesnesine de ekle
        customer.addAccount(account);

        return account;
    }

    public void deposit(String accountNumber, double amount, String description) {
        Account account = accountService.findAccount(accountNumber);
        account.deposit(amount, description);
        FileManager.log("Para yatırma: " + accountNumber + " | " + amount + " TL");
    }

    public void withdraw(String accountNumber, double amount, String description) {
        Account account = accountService.findAccount(accountNumber);
        account.withdraw(amount, description);
        FileManager.log("Para çekme: " + accountNumber + " | " + amount + " TL");
    }

    public void transfer(String fromAccount, String toAccount, double amount) {
        Account source = accountService.findAccount(fromAccount);
        Account target = accountService.findAccount(toAccount);
        source.transfer(target, amount);
        FileManager.log("Transfer: " + fromAccount + " → " + toAccount + " | " + amount + " TL");
    }

    // ================================================================
    // LİSTELEME — STREAM API ile sıralama
    // ================================================================

    /**
     * Tüm müşterileri listele.
     * Collections ile sıralama.
     */
    public void listAllCustomers() {
        if (customers.isEmpty()) {
            System.out.println("Kayıtlı müşteri yok.");
            return;
        }

        System.out.println("\n=== TÜM MÜŞTERİLER ===");
        System.out.printf("%-12s | %-22s | %7s | %14s | %s%n",
            "ID", "Ad Soyad", "Hesap", "Toplam Bakiye", "Durum");
        System.out.println("-".repeat(70));

        // LAMBDA + SORT: müşterileri soyada göre sırala
        customers.stream()
            .sorted(Comparator.comparing(Customer::getLastName))
            .forEach(Customer::printSummary); // method reference

        System.out.println("-".repeat(70));
        System.out.printf("Toplam %d müşteri | Banka bakiyesi: %.2f TL%n",
            customers.size(), accountService.getTotalBankBalance());
    }

    /**
     * Tüm hesapları listele.
     */
    public void listAllAccounts() {
        List<Account> allAccounts = accountService.getAllAccounts();

        if (allAccounts.isEmpty()) {
            System.out.println("Kayıtlı hesap yok.");
            return;
        }

        System.out.println("\n=== TÜM HESAPLAR ===");
        System.out.printf("%-12s | %-25s | %14s | %s%n",
            "Hesap No", "Tür", "Bakiye", "Durum");
        System.out.println("-".repeat(65));

        // STREAM + sorted: bakiyeye göre büyükten küçüğe sırala
        allAccounts.stream()
            .sorted((a, b) -> Double.compare(b.getBalance(), a.getBalance()))
            .forEach(Account::printSummary);

        System.out.println("-".repeat(65));
    }

    /**
     * İşlem geçmişini dosyaya kaydet ve oku.
     */
    public void saveAndPrintHistory() {
        List<Account> allAccounts = accountService.getAllAccounts();
        FileManager.saveAllTransactions(allAccounts);
        FileManager.printFileContents("data/islem_gecmisi.txt");
    }

    // Getter'lar
    public AccountService getAccountService() { return accountService; }
    public LinkedList<Customer> getCustomers() { return customers; }
    public String getBankName() { return bankName; }
}
