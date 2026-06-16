package com.banka.exception;

/**
 * Hesap bulunamadığında fırlatılan özel exception.
 * Aranılan hesap numarasını da taşır — debug için faydalı.
 */
public class AccountNotFoundException extends RuntimeException {

    private final String accountNumber; // Bulunamayan hesap numarası

    // Constructor: hesap numarasını alır, açıklayıcı mesaj üretir
    public AccountNotFoundException(String accountNumber) {
        super("Hesap bulunamadı: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    // Mesajla birlikte hesap numarasını da alan overloaded constructor
    public AccountNotFoundException(String accountNumber, String additionalInfo) {
        super("Hesap bulunamadı: " + accountNumber + " - " + additionalInfo);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
