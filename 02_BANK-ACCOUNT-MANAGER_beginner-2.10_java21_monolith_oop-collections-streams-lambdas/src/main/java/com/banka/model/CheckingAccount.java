package com.banka.model;

import com.banka.enums.AccountType;
import com.banka.exception.InsufficientFundsException;

/**
 * Vadesiz (Çek) Hesabı — Account'tan türetilmiş ikinci alt sınıf.
 *
 * Özellikler:
 *   - Minimum bakiye zorunluluğu YOK
 *   - Overdraft (eksi bakiyeye düşme) belirli limite kadar izinli
 *   - İşlem başına komisyon kesilir
 */
public class CheckingAccount extends Account {

    private static final double OVERDRAFT_LIMIT = -1000.0;  // -1000 TL'ye kadar eksi olabilir
    private static final double TRANSACTION_FEE = 2.50;      // Her işlemde 2.50 TL komisyon

    private int transactionCount; // Bu aydaki işlem sayısı (komisyon hesabı için)

    public CheckingAccount(String customerId, double initialBalance) {
        // super() ile AccountType.CHECKING geçiriyoruz — polymorphism başlar burada
        super(customerId, AccountType.CHECKING, initialBalance);
        this.transactionCount = 0;
    }

    /**
     * Vadesiz hesap çekim kuralı:
     * Sadece overdraft limitini aşmamalı.
     * Minimum bakiye yok, negatife düşebilir (belirli limite kadar).
     */
    @Override
    protected void checkWithdrawEligibility(double amount) {
        double resultingBalance = getBalance() - amount;

        // Overdraft (eksi bakiye) limiti kontrolü
        if (resultingBalance < OVERDRAFT_LIMIT) {
            throw new InsufficientFundsException(
                "Overdraft limiti aşılıyor! Mevcut: " +
                String.format("%.2f", getBalance()) + " TL, " +
                "Limit: " + String.format("%.2f", OVERDRAFT_LIMIT) + " TL"
            );
        }

        // Komisyon kesimi — her çekim işleminde
        applyTransactionFee();
    }

    // Komisyon uygula
    private void applyTransactionFee() {
        // 5'ten fazla işlemde komisyon kes (ilk 5 ücretsiz)
        if (transactionCount >= 5) {
            double currentBalance = getBalance();
            // setBalance → protected: alt sınıf çağırabilir (access modifier)
            setBalance(currentBalance - TRANSACTION_FEE);
            System.out.printf("  [Komisyon] %.2f TL kesildi.%n", TRANSACTION_FEE);
        }
        transactionCount++;
    }

    /**
     * Vadesiz hesap düşük faiz kazanır.
     */
    @Override
    public double calculateInterest() {
        // Negatif bakiyede faiz hesaplanmaz
        if (getBalance() <= 0) {
            return 0;
        }
        return getBalance() * getAccountType().getInterestRate();
    }

    // Aylık sayacı sıfırla
    public void resetMonthlyCounter() {
        this.transactionCount = 0;
    }

    public int getTransactionCount() { return transactionCount; }
    public static double getOverdraftLimit() { return OVERDRAFT_LIMIT; }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.printf("Overdraft Limiti : %.2f TL%n", OVERDRAFT_LIMIT);
        System.out.printf("İşlem Başına Kom.: %.2f TL (6. işlemden itibaren)%n", TRANSACTION_FEE);
        System.out.printf("Bu Ay İşlem Sayısı: %d%n", transactionCount);
    }
}
