package com.concurrency.async;

import java.util.*;
import java.util.concurrent.*;

/**
 * COMPLETABLE FUTURE — Asenkron Programlama
 *
 * Future'ın eksikleri:
 *   - get() blocking
 *   - Zincirleme yok
 *   - Birleştirme yok
 *   - Exception handling zor
 *
 * CompletableFuture çözümleri:
 *   thenApply     → dönüşüm (Function)
 *   thenAccept    → tüketim (Consumer)
 *   thenRun       → aksiyon (Runnable)
 *   thenCompose   → zincirleme (flatMap gibi)
 *   thenCombine   → iki sonucu birleştir
 *   allOf         → hepsinin bitmesini bekle
 *   anyOf         → ilk bitenin sonucunu al
 *   exceptionally → hata yönetimi
 */
public class CompletableFutureDemo {

    // Simülasyon servisleri
    static CompletableFuture<String> fetchUser(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "User-" + userId;
        });
    }

    static CompletableFuture<String> fetchOrder(String user) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(150);
            return user + "-Order#42";
        });
    }

    static CompletableFuture<Double> fetchPrice(String order) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return 299.90;
        });
    }

    // ----------------------------------------------------------------
    // 1. Temel Zincir — thenApply, thenAccept
    // ----------------------------------------------------------------
    static void basicChainDemo() throws Exception {
        System.out.println("\n--- Temel Zincir ---");

        CompletableFuture<String> cf = CompletableFuture
                .supplyAsync(() -> "merhaba")           // async başlat
                .thenApply(String::toUpperCase)          // dönüştür
                .thenApply(s -> s + " DÜNYA");           // tekrar dönüştür

        System.out.println(cf.get());
    }

    // ----------------------------------------------------------------
    // 2. thenCompose — Zincirleme (flatMap)
    // ----------------------------------------------------------------
    static void composeDemo() throws Exception {
        System.out.println("\n--- thenCompose (Pipeline) ---");

        CompletableFuture<Double> pipeline = fetchUser(1)
                .thenCompose(user -> fetchOrder(user))
                .thenCompose(order -> fetchPrice(order))
                .thenApply(price -> price * 1.18); // KDV ekle

        System.out.printf("Toplam fiyat (KDV dahil): %.2f%n", pipeline.get());
    }

    // ----------------------------------------------------------------
    // 3. thenCombine — İki bağımsız görevi birleştir
    // ----------------------------------------------------------------
    static void combineDemo() throws Exception {
        System.out.println("\n--- thenCombine ---");

        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "Ahmet";
        });

        CompletableFuture<String> roleFuture = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "ADMIN";
        });

        // İkisi eş zamanlı çalışır, ikisi bitince birleşir
        String result = userFuture.thenCombine(roleFuture,
                (user, role) -> user + " [" + role + "]").get();

        System.out.println("Birleşik sonuç: " + result);
    }

    // ----------------------------------------------------------------
    // 4. allOf — Hepsinin bitmesini bekle
    // ----------------------------------------------------------------
    static void allOfDemo() throws Exception {
        System.out.println("\n--- allOf ---");

        List<CompletableFuture<String>> futures = List.of(
                CompletableFuture.supplyAsync(() -> { sleep(300); return "Servis-A"; }),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "Servis-B"; }),
                CompletableFuture.supplyAsync(() -> { sleep(200); return "Servis-C"; })
        );

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        allDone.thenRun(() -> {
            List<String> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            System.out.println("Tüm sonuçlar: " + results);
        }).get();
    }

    // ----------------------------------------------------------------
    // 5. anyOf — İlk bitenin sonucu
    // ----------------------------------------------------------------
    static void anyOfDemo() throws Exception {
        System.out.println("\n--- anyOf ---");

        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(500); return "Yavaş"; }),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "Hızlı"; }),
                CompletableFuture.supplyAsync(() -> { sleep(300); return "Orta"; })
        );

        System.out.println("İlk biten: " + fastest.get());
    }

    // ----------------------------------------------------------------
    // 6. Exception Handling
    // ----------------------------------------------------------------
    static void exceptionDemo() throws Exception {
        System.out.println("\n--- Exception Handling ---");

        // exceptionally — hata olursa varsayılan değer
        CompletableFuture<String> withFallback = CompletableFuture
                .supplyAsync(() -> {
                    if (Math.random() > 0.5) throw new RuntimeException("Servis hatası!");
                    return "Başarılı sonuç";
                })
                .exceptionally(ex -> {
                    System.out.println("Hata yakalandı: " + ex.getMessage());
                    return "Varsayılan değer";
                });

        System.out.println("Sonuç: " + withFallback.get());

        // handle — hem başarı hem hata durumunu yönet
        CompletableFuture<String> withHandle = CompletableFuture
                .supplyAsync(() -> { throw new RuntimeException("DB bağlantı hatası"); })
                .handle((result, ex) -> {
                    if (ex != null) return "Cache'den döndü: cached-data";
                    return result;
                });

        System.out.println("Handle sonuç: " + withHandle.get());
    }

    // ----------------------------------------------------------------
    // 7. Gerçek Dünya Örneği — E-commerce checkout
    // ----------------------------------------------------------------
    static void ecommerceCheckout() throws Exception {
        System.out.println("\n--- E-commerce Checkout Pipeline ---");

        long start = System.currentTimeMillis();

        CompletableFuture<String> stockCheck = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "Stok OK";
        });

        CompletableFuture<String> paymentCheck = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "Ödeme OK";
        });

        CompletableFuture<String> addressCheck = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            return "Adres OK";
        });

        // 3 bağımsız kontrol eş zamanlı çalışır
        String result = CompletableFuture.allOf(stockCheck, paymentCheck, addressCheck)
                .thenApply(v -> String.format("Checkout tamamlandı: %s, %s, %s",
                        stockCheck.join(), paymentCheck.join(), addressCheck.join()))
                .get();

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("%s (%dms)%n", result, elapsed);
        // Sıralı olsaydı: 650ms, paralel: ~300ms
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== COMPLETABLE FUTURE ===");
        basicChainDemo();
        composeDemo();
        combineDemo();
        allOfDemo();
        anyOfDemo();
        exceptionDemo();
        ecommerceCheckout();
    }
}
