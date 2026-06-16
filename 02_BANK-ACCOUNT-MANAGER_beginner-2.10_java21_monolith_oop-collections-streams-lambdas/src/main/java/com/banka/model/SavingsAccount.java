package com.banka.model;

import com.banka.enums.AccountType;
import com.banka.exception.InsufficientFundsException;

/**
 * INHERITANCE (Kalıtım) VE POLYMORPHISM ÖRNEĞİ:
 *
 * SavingsAccount → Account → (Transactable, Printable)
 *
 * extends: Account sınıfının tüm özelliklerini miras alır.
 * super(): üst sınıfın constructor'ını çağırır — ZORUNLU, ilk satır olmalı.
 *
 * Tasarruf Hesabı özellikleri:
 *   - Minimum bakiye tutma zorunluluğu
 *   - Günlük çekim limiti
 *   - Faiz kazanır (hesap türüne göre)
 */
public class SavingsAccount extends Account {

    // Tasarruf hesabına özgü sabitler
    // final + static: sınıf sabiti — tüm hesaplar için aynı kural
    private static final double MINIMUM_BALANCE = 100.0; // Minimum 100 TL
    private static final double DAILY_WITHDRAWAL_LIMIT = 5000.0; // Günlük 5000 TL

    // Bu hesaba özgü alan (Account'ta yok)
    private double dailyWithdrawnAmount; // Bugün çekilen toplam miktar

    /**
     * Constructor — super() ile üst sınıfa bilgileri iletir.
     */
    public SavingsAccount(String customerId, double initialBalance) {
        // super(): Account sınıfının protected constructor'ını çağır
        super(customerId, AccountType.SAVINGS, initialBalance);
        this.dailyWithdrawnAmount = 0.0;
    }

    /**
     * POLYMORPHISM — METHOD OVERRIDING:
     * Account sınıfının abstract metodunu burada implement ediyoruz.
     * @Override: derleyiciye "üst sınıfı override ediyorum" der
     *
     * Tasarruf hesabında çekim kuralları:
     *   1. Bakiye minimum bakiyenin altına düşemez
     *   2. Günlük çekim limitini aşamaz
     */
    @Override
    protected void checkWithdrawEligibility(double amount) {
        double resultingBalance = getBalance() - amount;

        // Kural 1: Minimum bakiye kontrolü
        if (resultingBalance < MINIMUM_BALANCE) {
            // InsufficientFundsException fırlat
            throw new InsufficientFundsException(
                getBalance() - MINIMUM_BALANCE, // çekilebilir maksimum
                amount
            );
        }

        // Kural 2: Günlük limit kontrolü
        if (dailyWithdrawnAmount + amount > DAILY_WITHDRAWAL_LIMIT) {
            double remaining = DAILY_WITHDRAWAL_LIMIT - dailyWithdrawnAmount;
            throw new InsufficientFundsException(
                "Günlük çekim limitini aştınız! " +
                "Kalan limit: " + String.format("%.2f", remaining) + " TL"
            );
        }

        // Kontroller geçtiyse günlük çekim miktarını güncelle
        dailyWithdrawnAmount += amount;
    }

    /**
     * Faiz hesaplama — tasarruf hesabı için basit faiz formülü.
     * Yıllık faiz oranı AccountType enum'undan gelir.
     */
    @Override
    public double calculateInterest() {
        // Faiz = Bakiye × Faiz Oranı (enum'dan alınır)
        double interestRate = getAccountType().getInterestRate();
        double interest = getBalance() * interestRate;
        return interest;
    }

    /**
     * METHOD OVERLOADING: Aynı isim, farklı parametre listesi.
     * applyInterest() → manuel faiz uygula
     * applyInterest(double customRate) → özel oran ile faiz uygula
     */
    public void applyInterest() {
        double interest = calculateInterest();
        deposit(interest, "Otomatik faiz: %" +
            (getAccountType().getInterestRate() * 100) + " yıllık");
    }

    // Overloaded versiyon: özel faiz oranıyla
    public void applyInterest(double customRate) {
        double interest = getBalance() * customRate;
        deposit(interest, "Özel faiz uygulandı: %" + (customRate * 100));
    }

    // Günlük çekim sayacını sıfırla (her gün gece yarısı çağrılır)
    public void resetDailyLimit() {
        this.dailyWithdrawnAmount = 0.0;
    }

    // Getter
    public double getDailyWithdrawnAmount() { return dailyWithdrawnAmount; }
    public static double getMinimumBalance() { return MINIMUM_BALANCE; }
    public static double getDailyWithdrawalLimit() { return DAILY_WITHDRAWAL_LIMIT; }

    // printDetails override — üst sınıfınkine ek bilgi ekle
    @Override
    public void printDetails() {
        super.printDetails(); // Üst sınıfın metodunu çağır, sonra ekle
        System.out.printf("Minimum Bakiye  : %.2f TL%n", MINIMUM_BALANCE);
        System.out.printf("Günlük Limit    : %.2f TL%n", DAILY_WITHDRAWAL_LIMIT);
        System.out.printf("Bugün Çekilen   : %.2f TL%n", dailyWithdrawnAmount);
        System.out.printf("Faiz Oranı (Yıl): %%%,.1f%n",
            getAccountType().getInterestRate() * 100);
    }
}
