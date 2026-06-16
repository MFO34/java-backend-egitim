package com.banka.model;

import com.banka.enums.TransactionType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bir hesap işlemini temsil eden sınıf.
 * Her para yatırma/çekme/transfer için bir TransactionRecord oluşturulur.
 *
 * ÖĞRENILEN KAVRAMLAR:
 *   - final field: değiştirilemez, immutable nesne
 *   - LocalDateTime: Java'nın modern tarih/saat sınıfı
 *   - static sayaç: tüm nesneler arasında paylaşılan ID üretici
 */
public class TransactionRecord {

    // static: sınıfa ait (nesneye değil), tüm işlemler arasında paylaşılır
    // Her yeni işlemde +1 artar → benzersiz ID üretir
    private static int transactionCounter = 0;

    // final: bir kez atanır, sonra değiştirilemez (immutable)
    private final int transactionId;          // Benzersiz işlem numarası
    private final String accountNumber;       // İşlemin yapıldığı hesap
    private final TransactionType type;       // İşlem türü (DEPOSIT, WITHDRAWAL vb.)
    private final double amount;              // İşlem miktarı
    private final double balanceAfter;        // İşlem sonrası bakiye
    private final LocalDateTime timestamp;   // İşlem tarihi ve saati
    private final String description;        // İşlem açıklaması

    // Constructor: tüm alanları dışarıdan alır
    public TransactionRecord(String accountNumber, TransactionType type,
                              double amount, double balanceAfter, String description) {
        // Static sayacı artır ve bu nesneye ata (benzersiz ID)
        this.transactionId = ++transactionCounter;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        // LocalDateTime.now() → şu anki tarih ve saat
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    // ---- GETTER METODLARI (Encapsulation) ----
    // private field'ları sadece okumak için public getter

    public int getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }

    // İşlemi tek satır metin olarak döner (dosyaya kaydetmek için)
    public String toCsvLine() {
        // DateTimeFormatter: tarihi istediğimiz formata çevirir
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        // String.join yerine "|" ayracıyla CSV formatı
        return transactionId + "|" +
               accountNumber + "|" +
               type.name() + "|" +
               String.format("%.2f", amount) + "|" +
               String.format("%.2f", balanceAfter) + "|" +
               timestamp.format(fmt) + "|" +
               description;
    }

    // Konsola yazdırmak için güzel format
    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return String.format("[#%04d] %s | %s %.2f TL | Bakiye: %.2f TL | %s | %s",
            transactionId,
            timestamp.format(fmt),
            type.getSymbol(),
            amount,
            balanceAfter,
            type.getDisplayName(),
            description
        );
    }
}
