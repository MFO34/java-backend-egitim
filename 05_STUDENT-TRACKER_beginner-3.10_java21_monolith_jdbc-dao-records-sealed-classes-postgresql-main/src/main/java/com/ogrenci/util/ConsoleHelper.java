package com.ogrenci.util;

import java.util.InputMismatchException;
import java.util.Scanner;

/** Konsol giriş/çıkış yardımcıları. */
public class ConsoleHelper {

    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleHelper() {}

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = scanner.nextInt();
                scanner.nextLine();
                return value;
            } catch (InputMismatchException e) {
                System.out.println("  Hata: Lütfen geçerli bir sayı girin.");
                scanner.nextLine();
            }
        }
    }

    public static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = scanner.nextDouble();
                scanner.nextLine();
                return value;
            } catch (InputMismatchException e) {
                System.out.println("  Hata: Geçerli bir sayı girin (örn: 85.5)");
                scanner.nextLine();
            }
        }
    }

    public static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (e/h): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("e") || input.equals("evet")) return true;
            if (input.equals("h") || input.equals("hayir") || input.equals("hayır")) return false;
            System.out.println("  'e' veya 'h' girin.");
        }
    }

    public static void printHeader(String title) {
        int width = 60;
        System.out.println("=".repeat(width));
        int pad = (width - title.length()) / 2;
        System.out.println(" ".repeat(Math.max(0, pad)) + title);
        System.out.println("=".repeat(width));
    }

    public static void waitForEnter() {
        System.out.print("\nDevam için Enter'a basın...");
        scanner.nextLine();
    }

    public static void close() { scanner.close(); }
}
