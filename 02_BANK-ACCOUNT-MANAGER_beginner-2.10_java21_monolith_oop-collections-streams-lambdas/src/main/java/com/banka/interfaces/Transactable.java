package com.banka.interfaces;

/**
 * INTERFACE (Arayüz) KAVRAMI:
 * Interface, "ne yapabilir" sorusuna cevap verir — "nasıl yapar" değil.
 * Bir sınıf interface'i "implement" ederse, o metodları YAZMak ZORUNDA.
 *
 * Abstract class vs Interface farkı:
 *   - Abstract class: "bir tür" ilişkisi  (SavingsAccount IS-A Account)
 *   - Interface:      "yetenek" ilişkisi  (Account CAN DO transactions)
 *
 * Java'da bir sınıf sadece 1 class'ı extend edebilir
 * ama birden fazla interface'i implement edebilir!
 */
public interface Transactable {

    // Interface metodları varsayılan olarak public ve abstract'tır
    // Yani her implement eden sınıf bunları yazmak ZORUNDA

    /**
     * Hesaba para yatırma işlemi.
     * @param amount Yatırılacak miktar (pozitif olmalı)
     * @param description İşlem açıklaması
     */
    void deposit(double amount, String description);

    /**
     * Hesaptan para çekme işlemi.
     * @param amount Çekilecek miktar
     * @param description İşlem açıklaması
     * @throws com.banka.exception.InsufficientFundsException Bakiye yetersizse
     * @throws com.banka.exception.InvalidAmountException Miktar geçersizse
     */
    void withdraw(double amount, String description);

    /**
     * Başka bir hesaba para transfer etme.
     * @param targetAccount Hedef hesap (Transactable interface'ini implemente etmeli)
     * @param amount Transfer miktarı
     */
    void transfer(Transactable targetAccount, double amount);

    /**
     * Güncel bakiyeyi döner.
     * @return Hesap bakiyesi
     */
    double getBalance();
}
