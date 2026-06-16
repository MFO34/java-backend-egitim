package com.banka.model;

import com.banka.enums.AccountType;
import com.banka.exception.InsufficientFundsException;
import com.banka.exception.InvalidAmountException;

/**
 * Kredi Hesabı — Account'tan türetilmiş üçüncü alt sınıf.
 *
 * POLYMORPHISM gösterimi:
 *   SavingsAccount, CheckingAccount, CreditAccount → hepsi Account türünde
 *   List<Account> içinde hepsi tutulabilir
 *   Ama withdraw() çağrıldığında HER BİRİ kendi kuralıyla çalışır
 *
 * Kredi hesabı kendine özgü mantık:
 *   - Kredi limiti var (bakiyeden bağımsız)
 *   - Para çekmek = kredi kullanmak (borçlanmak)
 *   - Para yatırmak = borç ödemek
 *   - Yüksek faiz oranı (borç için)
 */
public class CreditAccount extends Account {

    private final double creditLimit;    // Toplam kredi limiti
    private double usedCredit;           // Kullanılan kredi miktarı

    // Kredi hesabı özel constructor
    public CreditAccount(String customerId, double creditLimit) {
        // Kredi hesabı başlangıç bakiyesi = 0, bakiye negatife gidebilir
        super(customerId, AccountType.CREDIT, 0.0);
        this.creditLimit = creditLimit;
        this.usedCredit = 0.0;
    }

    /**
     * Kredi hesabında çekim = kredi kullanımı.
     * Kural: kullanılan kredi, kredi limitini aşamaz.
     */
    @Override
    protected void checkWithdrawEligibility(double amount) {
        double newUsedCredit = usedCredit + amount;

        // Kredi limiti kontrolü
        if (newUsedCredit > creditLimit) {
            double available = creditLimit - usedCredit;
            throw new InsufficientFundsException(
                "Kredi limitinizi aştınız! Kullanılabilir limit: " +
                String.format("%.2f", available) + " TL"
            );
        }

        // Limit aşılmıyorsa kullanılan krediyi artır
        usedCredit = newUsedCredit;
    }

    /**
     * Kredi hesabında para yatırma = borç ödeme.
     * override: üst sınıfın deposit'ini özelleştiriyoruz.
     */
    @Override
    public void deposit(double amount, String description) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        // Ödenen borcu usedCredit'ten düş
        usedCredit = Math.max(0, usedCredit - amount);
        // Üst sınıfın deposit işlemini de yap (işlem geçmişi için)
        super.deposit(amount, description);
    }

    /**
     * Kredi faizi — kullanılan kredi üzerinden hesaplanır.
     * Faiz oranı yüksek (hesap türünde %15 tanımladık).
     */
    @Override
    public double calculateInterest() {
        // Kullanılan kredi yoksa faiz yok
        if (usedCredit == 0) return 0;
        return usedCredit * getAccountType().getInterestRate();
    }

    // Kullanılabilir kredi miktarı
    public double getAvailableCredit() {
        return creditLimit - usedCredit;
    }

    public double getCreditLimit() { return creditLimit; }
    public double getUsedCredit() { return usedCredit; }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.printf("Toplam Limit      : %.2f TL%n", creditLimit);
        System.out.printf("Kullanılan Kredi  : %.2f TL%n", usedCredit);
        System.out.printf("Kullanılabilir    : %.2f TL%n", getAvailableCredit());
        System.out.printf("Aylık Faiz Oranı  : %%%,.1f%n",
            getAccountType().getInterestRate() * 100);
    }
}
