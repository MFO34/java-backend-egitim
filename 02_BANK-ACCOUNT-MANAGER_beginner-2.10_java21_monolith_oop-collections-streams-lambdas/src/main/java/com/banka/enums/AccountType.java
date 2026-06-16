package com.banka.enums;

/**
 * ENUM KAVRAMI:
 * Enum (Enumeration = Sayım/Liste), sabit değerler kümesi tanımlar.
 * Normal int/String yerine enum kullanmak:
 *   - Tip güvenliği sağlar (yanlış değer girilemez)
 *   - Kodu okunabilir kılar ("SAVINGS" > 1)
 *   - Switch-case ile mükemmel çalışır
 */
public enum AccountType {

    // Her enum sabiti: parantez içinde Türkçe açıklama ve faiz oranı
    SAVINGS("Tasarruf Hesabı", 0.05),        // Yıllık %5 faiz
    CHECKING("Vadesiz (Çek) Hesabı", 0.01),  // Yıllık %1 faiz
    CREDIT("Kredi Hesabı", 0.15);            // Yıllık %15 faiz

    // final: enum sabitleri değiştirilemez olmalı
    private final String displayName; // Kullanıcıya gösterilecek ad
    private final double interestRate; // Faiz oranı

    // Enum constructor'ı: her sabit oluşturulurken çağrılır
    AccountType(String displayName, double interestRate) {
        this.displayName = displayName;
        this.interestRate = interestRate;
    }

    // Getter metodları — encapsulation prensibi
    public String getDisplayName() {
        return displayName;
    }

    public double getInterestRate() {
        return interestRate;
    }

    // Enum'u numaradan bulmak için yardımcı metod
    public static AccountType fromOrdinal(int ordinal) {
        // values() → tüm enum sabitlerini dizi olarak döner
        AccountType[] types = AccountType.values();
        // Geçerli aralık kontrolü
        if (ordinal >= 0 && ordinal < types.length) {
            return types[ordinal];
        }
        // Geçersiz değer için null döner (çağıran kontrol etmeli)
        return null;
    }

    // toString override: yazdırıldığında güzel görünsün
    @Override
    public String toString() {
        return displayName;
    }
}
