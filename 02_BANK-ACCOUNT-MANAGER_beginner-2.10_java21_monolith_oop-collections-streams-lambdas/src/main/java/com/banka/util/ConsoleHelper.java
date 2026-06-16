package com.banka.util;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Konsol giriş/çıkış yardımcı sınıfı.
 *
 * ÖĞRENILEN KAVRAMLAR:
 *   - static utility class
 *   - Scanner ile kullanıcı girişi
 *   - try/catch ile giriş doğrulama
 */
public class ConsoleHelper {

    // Scanner: System.in'den (klavyeden) okuma
    // static: tek bir Scanner nesnesi tüm uygulama için yeterli
    private static final Scanner scanner = new Scanner(System.in);

    // Utility sınıf — nesne oluşturulmamalı
    private ConsoleHelper() {}

    /**
     * Kullanıcıdan integer girişi al.
     * Geçersiz giriş olursa tekrar ister.
     */
    public static int readInt(String prompt) {
        while (true) { // Geçerli giriş alana kadar dön
            System.out.print(prompt);
            try {
                int value = scanner.nextInt();
                scanner.nextLine(); // Buffer temizle (satır sonu karakterini atla)
                return value;
            } catch (InputMismatchException e) {
                // Sayı dışında bir şey girildi
                System.out.println("  Hata: Lütfen geçerli bir sayı girin.");
                scanner.nextLine(); // Yanlış girişi temizle
            }
        }
    }

    /**
     * Kullanıcıdan double girişi al.
     */
    public static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = scanner.nextDouble();
                scanner.nextLine(); // Buffer temizle
                return value;
            } catch (InputMismatchException e) {
                System.out.println("  Hata: Lütfen geçerli bir miktar girin (örn: 100.50)");
                scanner.nextLine();
            }
        }
    }

    /**
     * Kullanıcıdan metin girişi al.
     */
    public static String readString(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        // trim(): baş ve son boşlukları temizle
        return input.trim();
    }

    /**
     * Kullanıcıdan evet/hayır girişi al.
     * boolean döner — true=evet, false=hayır
     */
    public static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (e/h): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("e") || input.equals("evet")) return true;
            if (input.equals("h") || input.equals("hayır") || input.equals("hayir")) return false;
            System.out.println("  Lütfen 'e' veya 'h' girin.");
        }
    }

    // Konsolu temizle — işletim sisteminden bağımsız
    public static void clearScreen() {
        System.out.println("\n".repeat(3));
    }

    // Ekrana başlık yazdır
    public static void printHeader(String title) {
        int width = 56;
        System.out.println("=".repeat(width));
        // Başlığı ortala
        int padding = (width - title.length()) / 2;
        System.out.println(" ".repeat(Math.max(0, padding)) + title);
        System.out.println("=".repeat(width));
    }

    // Devam etmek için Enter bekle
    public static void waitForEnter() {
        System.out.print("\nDevam etmek için Enter'a basın...");
        scanner.nextLine();
    }

    // Scanner'ı kapat (uygulama kapanırken çağrılmalı)
    public static void close() {
        scanner.close();
    }
}
