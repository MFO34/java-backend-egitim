package com.banka.enums;

/**
 * İşlem türlerini temsil eden enum.
 * Her finansal işlem bu türlerden biri olacak.
 */
public enum TransactionType {

    DEPOSIT("Para Yatırma", "+"),    // Hesaba para ekleme
    WITHDRAWAL("Para Çekme", "-"),  // Hesaptan para çekme
    TRANSFER("Transfer", "⇄"),      // Hesaplar arası para aktarma
    INTEREST("Faiz Ekleme", "+");   // Otomatik faiz işlemi

    private final String displayName; // Kullanıcıya gösterilecek işlem adı
    private final String symbol;      // İşlemi sembolize eden karakter

    // Enum constructor
    TransactionType(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol + " " + displayName;
    }
}
