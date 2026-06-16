package com.banka.util;

import com.banka.model.Account;
import com.banka.model.TransactionRecord;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE I/O KAVRAMI:
 *
 * Java'da dosya işlemleri iki temel yolla yapılır:
 *   1. Karakter tabanlı: FileWriter, BufferedWriter, FileReader, BufferedReader
 *   2. Bayt tabanlı: FileInputStream, FileOutputStream
 *
 * Modern yol: java.nio.file.Files (daha temiz API)
 *
 * TRY-WITH-RESOURCES:
 *   try (BufferedWriter bw = new BufferedWriter(...)) { ... }
 *   Blok bitince otomatik kapatır — finally yazmaya gerek yok!
 *
 * Tüm dosya işlemleri EXCEPTION fırlatabilir → try/catch zorunlu.
 */
public class FileManager {

    // Dosya yolları — final static sabitler
    private static final String DATA_DIR = "data";              // Klasör adı
    private static final String TRANSACTIONS_FILE = DATA_DIR + "/islem_gecmisi.txt";
    private static final String LOG_FILE = DATA_DIR + "/banka_log.txt";

    // DateTimeFormatter: tarih/saat formatı
    private static final DateTimeFormatter FILE_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    /**
     * Data klasörünü oluştur (yoksa).
     * static metod: nesne oluşturmadan çağrılabilir.
     */
    public static void initializeDataDirectory() {
        // Path: dosya yolunu temsil eden nesne
        Path dataPath = Paths.get(DATA_DIR);

        // Files.exists(): dosya/klasör var mı kontrolü
        if (!Files.exists(dataPath)) {
            try {
                // Files.createDirectory(): klasör oluştur
                Files.createDirectory(dataPath);
                System.out.println("Data klasörü oluşturuldu: " + DATA_DIR);
            } catch (IOException e) {
                // IOException: I/O işlemlerinde oluşabilecek hata
                System.err.println("Klasör oluşturulamadı: " + e.getMessage());
            }
        }
    }

    /**
     * İşlem geçmişini dosyaya yaz.
     *
     * @param account Geçmişi yazılacak hesap
     */
    public static void saveTransactionHistory(Account account) {
        initializeDataDirectory();

        // TRY-WITH-RESOURCES: otomatik kaynak kapatma
        // BufferedWriter: büyük dosyalar için verimli yazma (bellekte tutar, sonra yazar)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
            // true parametresi: append mode — mevcut içeriğin üstüne yaz

            // Başlık satırı
            writer.write("=== HESAP: " + account.getAccountNumber() + " | " +
                         LocalDateTime.now().format(FILE_DATE_FORMAT) + " ===");
            writer.newLine(); // Yeni satır

            // Her işlemi CSV formatında yaz
            List<TransactionRecord> history = account.getTransactionHistory();
            for (TransactionRecord record : history) {
                writer.write(record.toCsvLine());
                writer.newLine();
            }
            writer.write("---");
            writer.newLine();

            System.out.println("İşlem geçmişi kaydedildi: " + TRANSACTIONS_FILE);

        } catch (IOException e) {
            // Hata mesajını stderr'e yaz
            System.err.println("Dosyaya yazma hatası: " + e.getMessage());
        }
    }

    /**
     * Tüm hesapların işlem geçmişini dosyaya yaz.
     * GENERICS: List<Account> — herhangi bir Account alt sınıfı çalışır
     */
    public static void saveAllTransactions(List<Account> accounts) {
        initializeDataDirectory();

        // Dosyayı baştan yaz (append değil)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE, false))) {

            writer.write("BANKA HESAP YÖNETİM SİSTEMİ - İŞLEM GEÇMİŞİ");
            writer.newLine();
            writer.write("Oluşturulma: " + LocalDateTime.now().format(FILE_DATE_FORMAT));
            writer.newLine();
            writer.write("=".repeat(70));
            writer.newLine();

            // Enhanced for loop ile tüm hesapları gez
            for (Account account : accounts) {
                writer.write("HESAP: " + account.getAccountNumber() +
                             " | TÜR: " + account.getAccountType().getDisplayName());
                writer.newLine();

                // Her hesabın işlemlerini yaz
                for (TransactionRecord record : account.getTransactionHistory()) {
                    writer.write("  " + record.toCsvLine());
                    writer.newLine();
                }
                writer.write("-".repeat(50));
                writer.newLine();
            }

        } catch (IOException e) {
            System.err.println("Dosya yazma hatası: " + e.getMessage());
        }
    }

    /**
     * İşlem geçmişini dosyadan oku.
     * RETURN TYPE: List<String> — her satır bir String
     */
    public static List<String> readTransactionHistory() {
        // ArrayList: sonuçları tutmak için
        List<String> lines = new ArrayList<>();

        // Dosya var mı?
        if (!Files.exists(Paths.get(TRANSACTIONS_FILE))) {
            System.out.println("İşlem geçmişi dosyası bulunamadı.");
            return lines; // Boş liste döner
        }

        // TRY-WITH-RESOURCES ile BufferedReader
        // BufferedReader: satır satır okuma için verimli
        try (BufferedReader reader = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {

            String line; // Okunan satırı tutan değişken

            // readLine(): null döndürünce dosya bitti demek
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

        } catch (FileNotFoundException e) {
            System.err.println("Dosya bulunamadı: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Dosya okuma hatası: " + e.getMessage());
        }

        return lines;
    }

    /**
     * Log dosyasına olay yaz.
     * Uygulama olaylarını takip etmek için.
     */
    public static void log(String event) {
        initializeDataDirectory();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            // String.format ile zaman damgalı log
            String logEntry = String.format("[%s] %s",
                LocalDateTime.now().format(FILE_DATE_FORMAT),
                event
            );
            writer.write(logEntry);
            writer.newLine();

        } catch (IOException e) {
            // Log hatası sessizce geç — kritik değil
        }
    }

    /**
     * Dosyanın içeriğini konsola yazdır.
     * FINALLY bloğu örneği — eski usul try/catch/finally
     */
    public static void printFileContents(String filePath) {
        BufferedReader reader = null; // try dışında tanımla

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            System.out.println("=== DOSYA İÇERİĞİ: " + filePath + " ===");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Dosya bulunamadı: " + filePath);
        } catch (IOException e) {
            System.err.println("Okuma hatası: " + e.getMessage());
        } finally {
            // FINALLY: hata olsa da olmasa da çalışır
            // Kaynağı kapat
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Kapatma hatası: " + e.getMessage());
                }
            }
            System.out.println("=== DOSYA OKUMA TAMAMLANDI ===");
        }
    }
}
