package com.concurrency.virtual;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * VIRTUAL THREADS — Java 21 (Project Loom)
 *
 * Platform Thread vs Virtual Thread:
 *   Platform Thread → OS thread, pahalı, ~1MB stack, binlerce oluşturmak zor
 *   Virtual Thread  → JVM thread, ucuz, ~KB stack, milyonlarca oluşturulabilir
 *
 * Ne zaman Virtual Thread kullanılır?
 *   ✓ I/O bound işlemler (HTTP, DB, dosya)
 *   ✗ CPU bound işlemler (hesaplama, şifreleme) → platform thread daha iyi
 *
 * Spring Boot 3.2+ → server.tomcat.threads.virtual=true ile otomatik aktif
 */
public class VirtualThreadsDemo {

    // ----------------------------------------------------------------
    // 1. Virtual Thread oluşturma yolları
    // ----------------------------------------------------------------
    static void creationDemo() throws Exception {
        System.out.println("\n--- Virtual Thread Oluşturma ---");

        // Yol 1: Thread.ofVirtual()
        Thread vt1 = Thread.ofVirtual()
                .name("vt-1")
                .start(() -> System.out.printf("[%s] virtual=%b%n",
                        Thread.currentThread().getName(),
                        Thread.currentThread().isVirtual()));

        // Yol 2: Thread.startVirtualThread()
        Thread vt2 = Thread.startVirtualThread(() ->
                System.out.println("[" + Thread.currentThread().getName() + "] Çalışıyor"));

        // Yol 3: ExecutorService (önerilen)
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> System.out.println("Virtual thread via executor"));
        }

        vt1.join(); vt2.join();
    }

    // ----------------------------------------------------------------
    // 2. Platform Thread vs Virtual Thread — Performans
    // ----------------------------------------------------------------
    static void performanceComparison() throws Exception {
        System.out.println("\n--- Platform vs Virtual Thread Performansı ---");
        int taskCount = 10_000;

        // Platform Thread
        long start = System.currentTimeMillis();
        try (ExecutorService executor = Executors.newFixedThreadPool(200)) {
            CountDownLatch latch = new CountDownLatch(taskCount);
            IntStream.range(0, taskCount).forEach(i -> executor.submit(() -> {
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { latch.countDown(); }
            }));
            latch.await();
        }
        System.out.printf("Platform Thread (%d görev): %dms%n",
                taskCount, System.currentTimeMillis() - start);

        // Virtual Thread
        start = System.currentTimeMillis();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(taskCount);
            IntStream.range(0, taskCount).forEach(i -> executor.submit(() -> {
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { latch.countDown(); }
            }));
            latch.await();
        }
        System.out.printf("Virtual Thread  (%d görev): %dms%n",
                taskCount, System.currentTimeMillis() - start);
    }

    // ----------------------------------------------------------------
    // 3. Structured Concurrency (Java 21 Preview)
    // İlgili görevlerin yaşam döngüsünü yönet
    // ----------------------------------------------------------------
    static void structuredConcurrencyDemo() throws Exception {
        System.out.println("\n--- Structured Concurrency ---");

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // İki bağımsız görevi paralel başlat
            StructuredTaskScope.Subtask<String> userTask =
                    scope.fork(() -> fetchUserData(1));
            StructuredTaskScope.Subtask<String> orderTask =
                    scope.fork(() -> fetchOrderData(1));

            scope.join()           // her ikisi bitene kadar bekle
                 .throwIfFailed(); // herhangi biri başarısız olursa exception fırlat

            System.out.println("Kullanıcı: " + userTask.get());
            System.out.println("Sipariş: " + orderTask.get());
        }
    }

    // ----------------------------------------------------------------
    // 4. Virtual Thread'lerle I/O Simulation
    // ----------------------------------------------------------------
    static void ioSimulation() throws Exception {
        System.out.println("\n--- I/O Simulation (1000 eş zamanlı istek) ---");
        long start = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(1000);

            for (int i = 0; i < 1000; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    try {
                        // DB sorgusu / HTTP isteği simülasyonu
                        Thread.sleep(Duration.ofMillis(100));
                        if (requestId % 100 == 0)
                            System.out.printf("İstek-%d tamamlandı [%s]%n",
                                    requestId, Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        System.out.printf("1000 istek tamamlandı: %dms%n",
                System.currentTimeMillis() - start);
    }

    // ----------------------------------------------------------------
    // 5. Virtual Thread Tuzakları (Pinning)
    // synchronized bloğu içinde virtual thread platform thread'e pin'lenir
    // → ReentrantLock kullan
    // ----------------------------------------------------------------
    static void pinningDemo() {
        System.out.println("\n--- Virtual Thread Tuzakları ---");
        System.out.println("SORUN: synchronized içinde virtual thread pin'lenir:");
        System.out.println("  synchronized(lock) { Thread.sleep(1000); }");
        System.out.println("  → virtual thread platform thread'e bağlanır, verimlilik düşer");
        System.out.println();
        System.out.println("ÇÖZÜM: ReentrantLock kullan:");
        System.out.println("  lock.lock(); try { Thread.sleep(1000); } finally { lock.unlock(); }");
        System.out.println("  → virtual thread mount/unmount yapabilir, verimli");
    }

    private static String fetchUserData(int id) throws InterruptedException {
        Thread.sleep(200); // I/O simülasyonu
        return "User-" + id;
    }

    private static String fetchOrderData(int id) throws InterruptedException {
        Thread.sleep(150);
        return "Order-" + id;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== VIRTUAL THREADS (Java 21) ===");
        creationDemo();
        performanceComparison();
        structuredConcurrencyDemo();
        ioSimulation();
        pinningDemo();
    }
}
