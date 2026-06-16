package com.patterns.behavioral.strategy;

import java.util.*;

/**
 * STRATEGY PATTERN — Algoritmaları değiştirilebilir hale getirme
 *
 * Kullanım: Sıralama stratejisi, ödeme yöntemi, fiyatlandırma,
 *           Spring Security AuthenticationProvider, Comparator
 *
 * OCP (Open/Closed Principle): Yeni strateji eklemek için mevcut kodu değiştirmez
 */
public class StrategyPattern {

    // ================================================================
    // 1. Ödeme Stratejisi
    // ================================================================
    interface PaymentStrategy {
        boolean pay(double amount);
        String getMethodName();
    }

    record CreditCardPayment(String cardNumber, String cvv) implements PaymentStrategy {
        @Override
        public boolean pay(double amount) {
            System.out.printf("[KREDI KARTI] %s ile %.2f TL ödendi%n",
                    cardNumber.substring(0, 4) + "****", amount);
            return true;
        }
        @Override public String getMethodName() { return "Kredi Kartı"; }
    }

    record BankTransferPayment(String iban) implements PaymentStrategy {
        @Override
        public boolean pay(double amount) {
            System.out.printf("[EFT] %s → %.2f TL%n", iban.substring(0, 8) + "...", amount);
            return true;
        }
        @Override public String getMethodName() { return "EFT"; }
    }

    record CryptoPayment(String walletAddress, String currency) implements PaymentStrategy {
        @Override
        public boolean pay(double amount) {
            System.out.printf("[CRYPTO] %.2f TL = ~%.6f %s → %s%n",
                    amount, amount / 3_000_000.0, currency, walletAddress.substring(0, 8) + "...");
            return true;
        }
        @Override public String getMethodName() { return "Crypto (" + currency + ")"; }
    }

    static class ShoppingCart {
        private final List<String> items = new ArrayList<>();
        private PaymentStrategy paymentStrategy;

        void addItem(String item)                   { items.add(item); }
        void setPaymentStrategy(PaymentStrategy ps) { this.paymentStrategy = ps; }

        void checkout(double totalAmount) {
            if (paymentStrategy == null) throw new IllegalStateException("Ödeme yöntemi seçilmedi");
            System.out.println("Sepet: " + items);
            System.out.println("Ödeme yöntemi: " + paymentStrategy.getMethodName());
            boolean success = paymentStrategy.pay(totalAmount);
            System.out.println("Sonuç: " + (success ? "✓ Başarılı" : "✗ Başarısız"));
        }
    }

    // ================================================================
    // 2. Fiyatlandırma Stratejisi (gerçek dünya: e-ticaret)
    // ================================================================
    @FunctionalInterface
    interface PricingStrategy {
        double calculatePrice(double basePrice, int quantity);
    }

    static class PricingEngine {
        static final PricingStrategy STANDARD      = (price, qty) -> price * qty;
        static final PricingStrategy BULK_DISCOUNT = (price, qty) -> {
            double discount = qty >= 100 ? 0.20 : qty >= 50 ? 0.10 : qty >= 10 ? 0.05 : 0.0;
            return price * qty * (1 - discount);
        };
        static final PricingStrategy SEASONAL      = (price, qty) -> price * qty * 0.85; // %15 sezon indirimi
        static final PricingStrategy VIP_MEMBER    = (price, qty) -> price * qty * 0.70; // %30 VIP indirimi

        private PricingStrategy strategy;

        PricingEngine(PricingStrategy strategy) { this.strategy = strategy; }
        void setStrategy(PricingStrategy s)     { this.strategy = s; }

        double calculate(double price, int qty) { return strategy.calculatePrice(price, qty); }
    }

    // ================================================================
    // 3. Sıralama Stratejisi (Comparator = Strategy)
    // ================================================================
    record Product(String name, double price, int stock, double rating) {}

    static void sortDemo() {
        List<Product> products = List.of(
                new Product("Laptop", 15000, 50, 4.5),
                new Product("Phone",   8000, 120, 4.8),
                new Product("Tablet",  5000, 30, 4.2),
                new Product("Watch",   3000, 80, 4.6)
        );

        Comparator<Product> byPrice  = Comparator.comparingDouble(Product::price);
        Comparator<Product> byRating = Comparator.comparingDouble(Product::rating).reversed();
        Comparator<Product> byStock  = Comparator.comparingInt(Product::stock).reversed();

        System.out.println("Fiyata göre: " + products.stream().sorted(byPrice).map(Product::name).toList());
        System.out.println("Puana göre: "  + products.stream().sorted(byRating).map(Product::name).toList());
        System.out.println("Stoğa göre: "  + products.stream().sorted(byStock).map(Product::name).toList());
    }

    public static void main(String[] args) {
        System.out.println("=== STRATEGY PATTERN ===\n");

        // Ödeme Stratejisi
        ShoppingCart cart = new ShoppingCart();
        cart.addItem("Laptop"); cart.addItem("Mouse");

        cart.setPaymentStrategy(new CreditCardPayment("4111111111111111", "123"));
        cart.checkout(15500.0);

        System.out.println();
        cart.setPaymentStrategy(new BankTransferPayment("TR33 0006 1005 1978 6457 8413 26"));
        cart.checkout(15500.0);

        System.out.println();
        cart.setPaymentStrategy(new CryptoPayment("0x742d35Cc6634C0532925a3b844Bc454e4438f44e", "ETH"));
        cart.checkout(15500.0);

        // Fiyatlandırma
        System.out.println("\n--- Fiyatlandırma Stratejisi ---");
        PricingEngine engine = new PricingEngine(PricingEngine.STANDARD);
        System.out.printf("Standart (10 adet × 100TL): %.2f%n", engine.calculate(100, 10));

        engine.setStrategy(PricingEngine.BULK_DISCOUNT);
        System.out.printf("Toplu indirim (100 adet × 100TL): %.2f%n", engine.calculate(100, 100));

        engine.setStrategy(PricingEngine.VIP_MEMBER);
        System.out.printf("VIP üye (10 adet × 100TL): %.2f%n", engine.calculate(100, 10));

        // Sıralama
        System.out.println("\n--- Sıralama Stratejisi ---");
        sortDemo();
    }
}
