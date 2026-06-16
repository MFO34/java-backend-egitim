package com.banka.model;

import com.banka.interfaces.Printable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Banka müşterisini temsil eder.
 *
 * ÖĞRENILEN KAVRAMLAR:
 *   - Encapsulation (private alanlar + getter/setter)
 *   - StringBuilder ile String birleştirme
 *   - Wrapper class kullanımı (Integer, Boolean)
 *   - List<Account> generics kullanımı
 */
public class Customer implements Printable {

    // ---- WRAPPER CLASS ÖRNEĞİ ----
    // int yerine Integer: null olabilir, Integer metodlarına erişebilir
    private static Integer customerCounter = 1000; // static: paylaşılan sayaç

    // private alanlar — encapsulation
    private final String customerId;       // Benzersiz müşteri ID
    private String firstName;              // Ad
    private String lastName;               // Soyad
    private String email;                  // E-posta
    private String phone;                  // Telefon
    private String nationalId;             // TC Kimlik Numarası
    private boolean active;                // Müşteri aktif mi?
    private final LocalDateTime createdAt; // Kayıt tarihi

    // GENERICS: List<Account> — sadece Account nesneleri tutar
    private final List<Account> accounts;

    // Constructor
    public Customer(String firstName, String lastName,
                    String email, String phone, String nationalId) {
        // Integer.toString() — wrapper class metodu kullanımı
        // AUTOBOXING: ++customerCounter (int) → Integer otomatik dönüşüm
        this.customerId = "CUS" + String.format("%05d", ++customerCounter);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.nationalId = nationalId;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.accounts = new ArrayList<>();
    }

    // Hesap ekle
    public void addAccount(Account account) {
        accounts.add(account);
    }

    // Hesap sil
    public boolean removeAccount(String accountNumber) {
        // enhanced for loop (for-each): koleksiyonu dolaşmak için
        for (Account account : accounts) {
            if (account.getAccountNumber().equals(accountNumber)) {
                accounts.remove(account);
                return true; // Bulundu ve silindi
            }
        }
        return false; // Bulunamadı
    }

    // Müşterinin toplam bakiyesi — tüm hesapların toplamı
    public double getTotalBalance() {
        double total = 0.0;
        // Enhanced for loop — ArrayList'i dolaş
        for (Account account : accounts) {
            // Sadece aktif hesapları say
            if (account.isActive()) {
                total += account.getBalance(); // UNBOXING: Double → double otomatik
            }
        }
        return total;
    }

    // ---- PRINTABLE INTERFACE ----

    @Override
    public void printSummary() {
        // StringBuilder: String birleştirme için verimli yol
        // "+" ile birleştirme her seferinde yeni String oluşturur — verimsiz!
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s", customerId));
        sb.append(" | ");
        sb.append(String.format("%-20s", getFullName()));
        sb.append(" | ");
        sb.append(String.format("%d hesap", accounts.size()));
        sb.append(" | ");
        sb.append(String.format("%12.2f TL", getTotalBalance()));
        sb.append(" | ");
        sb.append(active ? "Aktif" : "Pasif");
        System.out.println(sb.toString());
    }

    @Override
    public void printDetails() {
        printSeparator();
        System.out.println("MÜŞTERİ DETAYLARI");
        System.out.println("ID          : " + customerId);
        System.out.println("Ad Soyad    : " + getFullName());
        System.out.println("E-posta     : " + email);
        System.out.println("Telefon     : " + phone);
        // String.substring() ve replaceAll() — String metodları
        String maskedId = nationalId.substring(0, 3) +
                          "*".repeat(5) +
                          nationalId.substring(8);
        System.out.println("TC Kimlik   : " + maskedId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        System.out.println("Kayıt Tarihi: " + createdAt.format(fmt));
        System.out.println("Durum       : " + (active ? "Aktif" : "Pasif"));
        System.out.printf ("Toplam Bakiye: %.2f TL%n", getTotalBalance());

        // Hesapları listele
        if (!accounts.isEmpty()) {
            System.out.println("\nHesaplar:");
            for (Account acc : accounts) {
                System.out.print("  ");
                acc.printSummary();
            }
        }
        printSeparator();
    }

    // ---- GETTER / SETTER METODLARI ----

    public String getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNationalId() { return nationalId; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Account> getAccounts() { return new ArrayList<>(accounts); } // defensive copy

    // Tam adı döner — String metodları örneği
    public String getFullName() {
        // String.trim(): baş/son boşlukları temizle
        // String.toUpperCase(): büyük harf (Türkçe locale)
        return firstName.trim() + " " + lastName.trim().toUpperCase();
    }

    // Setter'lar — validation ile
    public void setFirstName(String firstName) {
        // isEmpty() — String metodu: boş mu kontrolü
        if (firstName != null && !firstName.isEmpty()) {
            this.firstName = firstName;
        }
    }

    public void setLastName(String lastName) {
        if (lastName != null && !lastName.isEmpty()) {
            this.lastName = lastName;
        }
    }

    public void setEmail(String email) {
        // contains() — String metodu: "@" içeriyor mu?
        if (email != null && email.contains("@")) {
            this.email = email;
        }
    }

    public void setPhone(String phone) { this.phone = phone; }
    public void setActive(boolean active) { this.active = active; }
}
