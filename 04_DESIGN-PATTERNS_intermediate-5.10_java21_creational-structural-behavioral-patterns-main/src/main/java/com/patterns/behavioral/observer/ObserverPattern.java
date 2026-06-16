package com.patterns.behavioral.observer;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OBSERVER PATTERN — Olay tabanlı bildirim
 *
 * Kullanım: Spring ApplicationEvent, Kafka consumer,
 *           GUI event listeners, WebSocket broadcast
 *
 * Push vs Pull:
 *   Push → Observer'a veri gönderilir (bu örnek)
 *   Pull → Observer kaynaktan veri çeker
 */
public class ObserverPattern {

    // ================================================================
    // Generic Observable altyapısı
    // ================================================================
    interface Observer<T> {
        void onEvent(T event);
    }

    static abstract class Observable<T> {
        // CopyOnWriteArrayList — iteration sırasında ekleme/çıkarma güvenli
        private final List<Observer<T>> observers = new CopyOnWriteArrayList<>();

        public void subscribe(Observer<T> observer)   { observers.add(observer); }
        public void unsubscribe(Observer<T> observer) { observers.remove(observer); }

        protected void notifyObservers(T event) {
            observers.forEach(obs -> obs.onEvent(event));
        }
    }

    // ================================================================
    // 1. E-ticaret Sipariş Olayları
    // ================================================================
    record OrderEvent(String orderId, String status, double amount, String customerEmail) {}

    static class OrderService extends Observable<OrderEvent> {
        public void placeOrder(String orderId, double amount, String email) {
            System.out.println("[OrderService] Sipariş oluşturuldu: " + orderId);
            notifyObservers(new OrderEvent(orderId, "PLACED", amount, email));
        }

        public void shipOrder(String orderId, double amount, String email) {
            System.out.println("[OrderService] Sipariş kargoya verildi: " + orderId);
            notifyObservers(new OrderEvent(orderId, "SHIPPED", amount, email));
        }

        public void cancelOrder(String orderId, double amount, String email) {
            System.out.println("[OrderService] Sipariş iptal edildi: " + orderId);
            notifyObservers(new OrderEvent(orderId, "CANCELLED", amount, email));
        }
    }

    // Observer'lar (Spring'de @EventListener ile aynı mantık)
    static class EmailNotifier implements Observer<OrderEvent> {
        @Override
        public void onEvent(OrderEvent event) {
            System.out.printf("[EMAIL] → %s: Sipariş %s, Durum: %s%n",
                    event.customerEmail(), event.orderId(), event.status());
        }
    }

    static class InventoryUpdater implements Observer<OrderEvent> {
        @Override
        public void onEvent(OrderEvent event) {
            if ("PLACED".equals(event.status())) {
                System.out.println("[INVENTORY] Stok rezerve edildi: " + event.orderId());
            } else if ("CANCELLED".equals(event.status())) {
                System.out.println("[INVENTORY] Stok serbest bırakıldı: " + event.orderId());
            }
        }
    }

    static class InvoiceGenerator implements Observer<OrderEvent> {
        @Override
        public void onEvent(OrderEvent event) {
            if ("PLACED".equals(event.status())) {
                System.out.printf("[INVOICE] Fatura oluşturuldu: %s → %.2f TL%n",
                        event.orderId(), event.amount());
            }
        }
    }

    static class FraudDetector implements Observer<OrderEvent> {
        @Override
        public void onEvent(OrderEvent event) {
            if (event.amount() > 10_000) {
                System.out.printf("[FRAUD] ⚠ Yüksek tutarlı işlem tespit edildi: %s → %.2f TL%n",
                        event.orderId(), event.amount());
            }
        }
    }

    // ================================================================
    // 2. Stock Price Observer (Gerçek zamanlı fiyat takibi)
    // ================================================================
    record StockEvent(String symbol, double price, double change) {}

    static class StockMarket extends Observable<StockEvent> {
        private final Map<String, Double> prices = new HashMap<>();

        public void updatePrice(String symbol, double newPrice) {
            double oldPrice = prices.getOrDefault(symbol, newPrice);
            double change = newPrice - oldPrice;
            prices.put(symbol, newPrice);
            notifyObservers(new StockEvent(symbol, newPrice, change));
        }
    }

    static class AlertSystem implements Observer<StockEvent> {
        private final double threshold;
        AlertSystem(double threshold) { this.threshold = threshold; }

        @Override
        public void onEvent(StockEvent event) {
            double changePercent = Math.abs(event.change() / (event.price() - event.change())) * 100;
            if (changePercent >= threshold) {
                System.out.printf("[ALERT] %s: %.2f%% değişim! Fiyat: %.2f%n",
                        event.symbol(), changePercent, event.price());
            }
        }
    }

    static class PortfolioTracker implements Observer<StockEvent> {
        private final Map<String, Integer> holdings = Map.of("THYAO", 100, "AKBNK", 200);

        @Override
        public void onEvent(StockEvent event) {
            Integer qty = holdings.get(event.symbol());
            if (qty != null) {
                double value = qty * event.price();
                System.out.printf("[PORTFOLIO] %s × %d = %.2f TL (Δ%.2f)%n",
                        event.symbol(), qty, value, qty * event.change());
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== OBSERVER PATTERN ===\n");

        // E-ticaret
        System.out.println("--- Sipariş Olayları ---");
        OrderService orderService = new OrderService();
        orderService.subscribe(new EmailNotifier());
        orderService.subscribe(new InventoryUpdater());
        orderService.subscribe(new InvoiceGenerator());
        orderService.subscribe(new FraudDetector());

        orderService.placeOrder("ORD-001", 5500.0, "alice@example.com");
        System.out.println();
        orderService.placeOrder("ORD-002", 15000.0, "bob@example.com"); // fraud alert
        System.out.println();
        orderService.cancelOrder("ORD-001", 5500.0, "alice@example.com");

        // Borsa
        System.out.println("\n--- Borsa Takip ---");
        StockMarket market = new StockMarket();
        market.subscribe(new AlertSystem(5.0)); // %5+ değişimde alert
        market.subscribe(new PortfolioTracker());

        market.updatePrice("THYAO", 100.0);
        market.updatePrice("THYAO", 108.0); // %8 artış → alert
        market.updatePrice("AKBNK", 50.0);
        market.updatePrice("AKBNK", 49.5);
    }
}
