package com.banka.app;

import com.banka.enums.AccountType;
import com.banka.exception.AccountNotFoundException;
import com.banka.exception.InsufficientFundsException;
import com.banka.exception.InvalidAmountException;
import com.banka.model.*;
import com.banka.service.BankService;
import com.banka.service.AccountService;
import com.banka.util.ConsoleHelper;
import com.banka.util.FileManager;

import java.util.List;

/**
 * MAIN CLASS — Uygulamanın giriş noktası.
 *
 * ÖĞRENILEN KAVRAMLAR:
 *   - main metodu: programın başladığı yer
 *   - while döngüsü: sonsuz menü döngüsü
 *   - switch-case: menü seçimi
 *   - try/catch: tüm exception'ları yakala
 *   - do-while: en az bir kez çalışacak döngü
 */
public class BankApp {

    // Uygulama genelinde tek BankService nesnesi (singleton benzeri)
    private static BankService bankService;

    /**
     * main metodu: JVM (Java Virtual Machine) programı buradan başlatır.
     * String[] args: komut satırı argümanları (şimdilik kullanmıyoruz)
     */
    public static void main(String[] args) {
        // Banka servisini başlat
        bankService = new BankService("ClaudeBank A.Ş.");

        // Başlangıç verilerini yükle (demo için)
        loadDemoData();

        // Ana menü döngüsü
        showMainMenu();
    }

    /**
     * ANA MENÜ — while(true) sonsuz döngü.
     * Kullanıcı "Çıkış" seçene kadar devam eder.
     */
    private static void showMainMenu() {
        boolean running = true; // Döngü kontrol bayrağı (flag)

        while (running) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.printHeader("CLAUDE BANK A.Ş. - ANA MENÜ");
            System.out.println("  1. Müşteri İşlemleri");
            System.out.println("  2. Hesap İşlemleri");
            System.out.println("  3. Para İşlemleri");
            System.out.println("  4. Sorgulama ve Raporlar");
            System.out.println("  5. Sistem İşlemleri");
            System.out.println("  0. Çıkış");
            System.out.println("=".repeat(56));

            int choice = ConsoleHelper.readInt("Seçiminiz: ");

            // SWITCH-CASE: menü seçimini yönet
            switch (choice) {
                case 1 -> showCustomerMenu();
                case 2 -> showAccountMenu();
                case 3 -> showTransactionMenu();
                case 4 -> showReportMenu();
                case 5 -> showSystemMenu();
                case 0 -> {
                    // Çıkıştan önce kaydet
                    System.out.println("\nİşlem geçmişi kaydediliyor...");
                    bankService.saveAndPrintHistory();
                    ConsoleHelper.close();
                    System.out.println("\nHoşça kalın! ClaudeBank'ı tercih ettiğiniz için teşekkürler.");
                    running = false; // Döngüyü bitir
                }
                default -> System.out.println("Geçersiz seçim. Lütfen tekrar deneyin.");
            }
        }
    }

    // ================================================================
    // MÜŞTERİ MENÜSÜ
    // ================================================================

    private static void showCustomerMenu() {
        // DO-WHILE: en az bir kez çalışır
        int choice;
        do {
            ConsoleHelper.printHeader("MÜŞTERİ İŞLEMLERİ");
            System.out.println("  1. Yeni Müşteri Ekle");
            System.out.println("  2. Müşteri Listele");
            System.out.println("  3. Müşteri Detayları");
            System.out.println("  4. Müşteri Ara");
            System.out.println("  5. Müşteri Sil");
            System.out.println("  0. Ana Menüye Dön");
            System.out.println("-".repeat(56));

            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> addCustomer();
                case 2 -> bankService.listAllCustomers();
                case 3 -> showCustomerDetails();
                case 4 -> searchCustomer();
                case 5 -> deleteCustomer();
                case 0 -> {} // Döngüden çık
                default -> System.out.println("Geçersiz seçim.");
            }

            if (choice != 0) ConsoleHelper.waitForEnter();

        } while (choice != 0); // choice 0 olana kadar dön
    }

    private static void addCustomer() {
        System.out.println("\n--- YENİ MÜŞTERİ EKLE ---");

        // Kullanıcıdan bilgileri al — try/catch ile hata yakala
        try {
            String firstName = ConsoleHelper.readString("Ad          : ");
            String lastName  = ConsoleHelper.readString("Soyad       : ");
            String email     = ConsoleHelper.readString("E-posta     : ");
            String phone     = ConsoleHelper.readString("Telefon     : ");
            String nationalId = ConsoleHelper.readString("TC Kimlik No: ");

            // TC Kimlik 11 hane olmalı
            if (nationalId.length() != 11) {
                System.out.println("Hata: TC Kimlik 11 hane olmalıdır.");
                return;
            }

            Customer customer = bankService.addCustomer(
                firstName, lastName, email, phone, nationalId);

            System.out.println("\nMüşteri başarıyla eklendi!");
            customer.printSummary();

        } catch (IllegalArgumentException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private static void showCustomerDetails() {
        String id = ConsoleHelper.readString("Müşteri ID : ");
        try {
            Customer customer = bankService.findCustomer(id);
            customer.printDetails();
        } catch (AccountNotFoundException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private static void searchCustomer() {
        String keyword = ConsoleHelper.readString("Arama kelimesi: ");
        List<Customer> results = bankService.searchCustomers(keyword);

        if (results.isEmpty()) {
            System.out.println("Sonuç bulunamadı: " + keyword);
        } else {
            System.out.println(results.size() + " müşteri bulundu:");
            // Enhanced for loop
            for (Customer c : results) {
                c.printSummary();
            }
        }
    }

    private static void deleteCustomer() {
        String id = ConsoleHelper.readString("Silinecek Müşteri ID: ");
        boolean confirm = ConsoleHelper.readYesNo("Emin misiniz?");
        if (confirm) {
            try {
                bankService.removeCustomer(id);
                System.out.println("Müşteri silindi.");
            } catch (AccountNotFoundException e) {
                System.out.println("Hata: " + e.getMessage());
            }
        }
    }

    // ================================================================
    // HESAP MENÜSÜ
    // ================================================================

    private static void showAccountMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("HESAP İŞLEMLERİ");
            System.out.println("  1. Yeni Hesap Aç");
            System.out.println("  2. Hesap Detayları");
            System.out.println("  3. Tüm Hesapları Listele");
            System.out.println("  4. Hesap Kapat");
            System.out.println("  5. Hesap İstatistikleri");
            System.out.println("  0. Ana Menüye Dön");
            System.out.println("-".repeat(56));

            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> openAccount();
                case 2 -> showAccountDetails();
                case 3 -> bankService.listAllAccounts();
                case 4 -> closeAccount();
                case 5 -> bankService.getAccountService().printAccountStats();
                case 0 -> {}
                default -> System.out.println("Geçersiz seçim.");
            }

            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    private static void openAccount() {
        System.out.println("\n--- YENİ HESAP AÇ ---");

        try {
            String customerId = ConsoleHelper.readString("Müşteri ID  : ");

            // Hesap türü seçimi — enum ile
            System.out.println("Hesap Türü:");
            System.out.println("  1. " + AccountType.SAVINGS.getDisplayName());
            System.out.println("  2. " + AccountType.CHECKING.getDisplayName());
            System.out.println("  3. " + AccountType.CREDIT.getDisplayName());

            int typeChoice = ConsoleHelper.readInt("Seçim (1-3) : ");

            // Enum.values() ile seçimi enum'a çevir
            AccountType type = AccountType.fromOrdinal(typeChoice - 1);
            if (type == null) {
                System.out.println("Geçersiz hesap türü.");
                return;
            }

            double amount;
            if (type == AccountType.CREDIT) {
                amount = ConsoleHelper.readDouble("Kredi Limiti (TL): ");
            } else {
                amount = ConsoleHelper.readDouble("Başlangıç Bakiyesi (TL): ");
            }

            Account account = bankService.openAccount(customerId, type, amount);
            System.out.println("\nHesap açıldı!");
            account.printDetails();

        } catch (AccountNotFoundException | InvalidAmountException | IllegalArgumentException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private static void showAccountDetails() {
        String accNo = ConsoleHelper.readString("Hesap No: ");
        try {
            Account account = bankService.getAccountService().findAccount(accNo);
            account.printDetails();
            account.printTransactionHistory();
        } catch (AccountNotFoundException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private static void closeAccount() {
        String accNo = ConsoleHelper.readString("Kapatılacak Hesap No: ");
        boolean confirm = ConsoleHelper.readYesNo("Hesabı kapatmak istediğinizden emin misiniz?");
        if (confirm) {
            try {
                bankService.getAccountService().closeAccount(accNo);
                System.out.println("Hesap kapatıldı: " + accNo);
            } catch (AccountNotFoundException e) {
                System.out.println("Hata: " + e.getMessage());
            }
        }
    }

    // ================================================================
    // PARA İŞLEMLERİ MENÜSÜ
    // ================================================================

    private static void showTransactionMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("PARA İŞLEMLERİ");
            System.out.println("  1. Para Yatır");
            System.out.println("  2. Para Çek");
            System.out.println("  3. Hesaplar Arası Transfer");
            System.out.println("  4. İşlem Geçmişi Görüntüle");
            System.out.println("  0. Ana Menüye Dön");
            System.out.println("-".repeat(56));

            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> performDeposit();
                case 2 -> performWithdrawal();
                case 3 -> performTransfer();
                case 4 -> viewTransactionHistory();
                case 0 -> {}
                default -> System.out.println("Geçersiz seçim.");
            }

            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    private static void performDeposit() {
        try {
            String accNo = ConsoleHelper.readString("Hesap No   : ");
            double amount = ConsoleHelper.readDouble("Miktar (TL): ");
            String desc = ConsoleHelper.readString("Açıklama   : ");

            bankService.deposit(accNo, amount, desc);

        } catch (AccountNotFoundException | InvalidAmountException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private static void performWithdrawal() {
        try {
            String accNo = ConsoleHelper.readString("Hesap No   : ");
            double amount = ConsoleHelper.readDouble("Miktar (TL): ");
            String desc = ConsoleHelper.readString("Açıklama   : ");

            bankService.withdraw(accNo, amount, desc);

        } catch (AccountNotFoundException | InsufficientFundsException | InvalidAmountException e) {
            // Birden fazla exception yakalanabilir (multi-catch)
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private static void performTransfer() {
        try {
            String from = ConsoleHelper.readString("Kaynak Hesap No : ");
            String to   = ConsoleHelper.readString("Hedef Hesap No  : ");
            double amount = ConsoleHelper.readDouble("Miktar (TL)     : ");

            bankService.transfer(from, to, amount);
            System.out.println("Transfer tamamlandı.");

        } catch (AccountNotFoundException | InsufficientFundsException | InvalidAmountException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private static void viewTransactionHistory() {
        String accNo = ConsoleHelper.readString("Hesap No: ");
        try {
            Account account = bankService.getAccountService().findAccount(accNo);
            account.printTransactionHistory();
        } catch (AccountNotFoundException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    // ================================================================
    // RAPOR MENÜSÜ
    // ================================================================

    private static void showReportMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("SORGULAMA VE RAPORLAR");
            System.out.println("  1. Tüm Müşteriler");
            System.out.println("  2. Tüm Hesaplar");
            System.out.println("  3. Hesap İstatistikleri");
            System.out.println("  4. En Yüksek Bakiyeli Hesap");
            System.out.println("  5. Belirli Bakiyenin Üzerindekiler");
            System.out.println("  0. Ana Menüye Dön");
            System.out.println("-".repeat(56));

            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> bankService.listAllCustomers();
                case 2 -> bankService.listAllAccounts();
                case 3 -> bankService.getAccountService().printAccountStats();
                case 4 -> {
                    bankService.getAccountService()
                        .getHighestBalanceAccount()
                        .ifPresent(acc -> {
                            System.out.println("En yüksek bakiyeli hesap:");
                            acc.printDetails();
                        });
                }
                case 5 -> {
                    double minBalance = ConsoleHelper.readDouble("Minimum bakiye (TL): ");
                    List<Account> accounts = bankService.getAccountService()
                        .getAccountsAboveBalance(minBalance);
                    System.out.println(accounts.size() + " hesap bulundu:");
                    accounts.forEach(Account::printSummary); // lambda + forEach
                }
                case 0 -> {}
                default -> System.out.println("Geçersiz seçim.");
            }

            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    // ================================================================
    // SİSTEM MENÜSÜ
    // ================================================================

    private static void showSystemMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("SİSTEM İŞLEMLERİ");
            System.out.println("  1. İşlem Geçmişini Dosyaya Kaydet");
            System.out.println("  2. Dosyadan İşlem Geçmişini Oku");
            System.out.println("  3. Banka Bilgileri");
            System.out.println("  0. Ana Menüye Dön");
            System.out.println("-".repeat(56));

            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> {
                    bankService.saveAndPrintHistory();
                    System.out.println("Kaydedildi.");
                }
                case 2 -> {
                    List<String> lines = FileManager.readTransactionHistory();
                    if (lines.isEmpty()) {
                        System.out.println("Dosya boş veya bulunamadı.");
                    } else {
                        lines.forEach(System.out::println); // method reference
                    }
                }
                case 3 -> {
                    ConsoleHelper.printHeader(bankService.getBankName());
                    System.out.println("Müşteri Sayısı : " + bankService.getCustomers().size());
                    bankService.getAccountService().printAccountStats();
                }
                case 0 -> {}
                default -> System.out.println("Geçersiz seçim.");
            }

            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    // ================================================================
    // DEMO VERİ — Uygulamayı test için hazır verilerle başlat
    // ================================================================

    /**
     * Uygulamayı demo verilerle başlatır.
     * FOR döngüsü, diziler, ve tüm özelliklerin testi.
     */
    private static void loadDemoData() {
        System.out.println("Demo veriler yükleniyor...");

        try {
            // ---- MÜŞTERİLER ----
            Customer ali = bankService.addCustomer(
                "Ali", "Yılmaz", "ali@email.com", "05001234567", "12345678901");
            Customer ayse = bankService.addCustomer(
                "Ayşe", "Kaya", "ayse@email.com", "05009876543", "98765432109");
            Customer mehmet = bankService.addCustomer(
                "Mehmet", "Demir", "mehmet@email.com", "05551112233", "11223344556");

            // ---- HESAPLAR ----
            // Ali'ye tasarruf ve vadesiz hesap
            Account aliSavings  = bankService.openAccount(
                ali.getCustomerId(), AccountType.SAVINGS, 5000.0);
            Account aliChecking = bankService.openAccount(
                ali.getCustomerId(), AccountType.CHECKING, 1000.0);

            // Ayşe'ye tasarruf ve kredi hesabı
            Account ayseSavings = bankService.openAccount(
                ayse.getCustomerId(), AccountType.SAVINGS, 15000.0);
            Account ayseCredit  = bankService.openAccount(
                ayse.getCustomerId(), AccountType.CREDIT, 10000.0); // 10000 TL limit

            // Mehmet'e vadesiz hesap
            Account mehmetChecking = bankService.openAccount(
                mehmet.getCustomerId(), AccountType.CHECKING, 3000.0);

            // ---- İŞLEMLER ----
            // FOR döngüsü ile çoklu işlem
            double[] depositAmounts = {500, 1000, 750}; // dizi tanımlama
            String[] descriptions = {"Maaş", "Kira geliri", "Serbest çalışma"};

            for (int i = 0; i < depositAmounts.length; i++) {
                // i: indis değişkeni, 0'dan başlar
                bankService.deposit(aliSavings.getAccountNumber(),
                    depositAmounts[i], descriptions[i]);
            }

            // Para çekme
            bankService.withdraw(aliSavings.getAccountNumber(), 300.0, "Market alışverişi");
            bankService.withdraw(aliChecking.getAccountNumber(), 200.0, "Fatura");

            // Transfer
            bankService.transfer(
                ayseSavings.getAccountNumber(),
                aliChecking.getAccountNumber(),
                500.0
            );

            // Kredi kullanımı
            bankService.withdraw(ayseCredit.getAccountNumber(), 2000.0, "Elektronik alışveriş");

            System.out.println("Demo veriler yüklendi. 3 müşteri, 5 hesap oluşturuldu.\n");

        } catch (Exception e) {
            System.out.println("Demo veri yüklenirken hata: " + e.getMessage());
        }
    }
}
