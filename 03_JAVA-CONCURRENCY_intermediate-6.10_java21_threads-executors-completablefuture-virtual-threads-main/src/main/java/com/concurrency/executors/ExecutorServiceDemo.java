package com.concurrency.executors;

import java.util.*;
import java.util.concurrent.*;

/**
 * EXECUTOR SERVICE — Thread Pool Yönetimi
 *
 * Thread'leri manuel yönetmek yerine havuz kullanmak:
 *   - Thread oluşturma maliyetini azaltır
 *   - Thread sayısını kontrol altında tutar
 *   - Görev kuyruğu sağlar
 *
 * Thread Pool Türleri:
 *   FixedThreadPool     → sabit sayıda thread
 *   CachedThreadPool    → dinamik, kısa süreli görevler için
 *   SingleThreadExecutor→ sıralı çalışma garantisi
 *   ScheduledThreadPool → zamanlanmış/periyodik görevler
 *   VirtualThreadPool   → Java 21, milyonlarca thread (sonraki dosyada)
 */
public class ExecutorServiceDemo {

    // ----------------------------------------------------------------
    // 1. FixedThreadPool
    // ----------------------------------------------------------------
    static void fixedThreadPoolDemo() throws InterruptedException {
        System.out.println("\n--- FixedThreadPool ---");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.printf("[%s] Görev-%d başladı%n", Thread.currentThread().getName(), taskId);
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.printf("[%s] Görev-%d bitti%n", Thread.currentThread().getName(), taskId);
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ----------------------------------------------------------------
    // 2. Future — asenkron sonuç bekleme
    // ----------------------------------------------------------------
    static void futureDemo() throws Exception {
        System.out.println("\n--- Future ---");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Integer> future1 = executor.submit(() -> {
            Thread.sleep(500);
            return 42;
        });

        Future<Integer> future2 = executor.submit(() -> {
            Thread.sleep(200);
            return 58;
        });

        System.out.println("Diğer işlemler yapılıyor...");

        int result1 = future1.get(2, TimeUnit.SECONDS); // timeout ile bekle
        int result2 = future2.get(2, TimeUnit.SECONDS);
        System.out.println("Toplam: " + (result1 + result2));

        executor.shutdown();
    }

    // ----------------------------------------------------------------
    // 3. invokeAll — tüm görevlerin bitmesini bekle
    // ----------------------------------------------------------------
    static void invokeAllDemo() throws Exception {
        System.out.println("\n--- invokeAll ---");
        ExecutorService executor = Executors.newFixedThreadPool(4);

        List<Callable<String>> tasks = List.of(
                () -> { Thread.sleep(300); return "Görev-A"; },
                () -> { Thread.sleep(100); return "Görev-B"; },
                () -> { Thread.sleep(200); return "Görev-C"; }
        );

        List<Future<String>> futures = executor.invokeAll(tasks);
        for (Future<String> f : futures) System.out.println("Sonuç: " + f.get());

        executor.shutdown();
    }

    // ----------------------------------------------------------------
    // 4. invokeAny — ilk biten görevi al
    // ----------------------------------------------------------------
    static void invokeAnyDemo() throws Exception {
        System.out.println("\n--- invokeAny ---");
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<String>> tasks = List.of(
                () -> { Thread.sleep(500); return "Yavaş Sunucu"; },
                () -> { Thread.sleep(100); return "Hızlı Sunucu"; },
                () -> { Thread.sleep(300); return "Orta Sunucu"; }
        );

        String fastest = executor.invokeAny(tasks); // ilk bitenin sonucu
        System.out.println("En hızlı: " + fastest);
        executor.shutdown();
    }

    // ----------------------------------------------------------------
    // 5. ScheduledExecutorService — Zamanlanmış görevler
    // ----------------------------------------------------------------
    static void scheduledDemo() throws InterruptedException {
        System.out.println("\n--- ScheduledExecutorService ---");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // Tek seferlik gecikme
        scheduler.schedule(() ->
                System.out.println("500ms sonra çalıştı"), 500, TimeUnit.MILLISECONDS);

        // Sabit aralıkla tekrar (fixedRate — önceki bitiş beklenmez)
        ScheduledFuture<?> fixedRate = scheduler.scheduleAtFixedRate(() ->
                System.out.printf("[%s] fixedRate%n", Thread.currentThread().getName()),
                0, 300, TimeUnit.MILLISECONDS);

        // Görev bitişinden sonra aralık (fixedDelay — önceki bitmesi beklenir)
        ScheduledFuture<?> fixedDelay = scheduler.scheduleWithFixedDelay(() ->
                System.out.printf("[%s] fixedDelay%n", Thread.currentThread().getName()),
                0, 300, TimeUnit.MILLISECONDS);

        Thread.sleep(1000);
        fixedRate.cancel(false);
        fixedDelay.cancel(false);
        scheduler.shutdown();
    }

    // ----------------------------------------------------------------
    // 6. ThreadPoolExecutor — Manuel yapılandırma
    // ----------------------------------------------------------------
    static void customThreadPoolDemo() throws InterruptedException {
        System.out.println("\n--- Custom ThreadPoolExecutor ---");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                              // corePoolSize
                4,                              // maximumPoolSize
                60, TimeUnit.SECONDS,           // keepAliveTime
                new ArrayBlockingQueue<>(10),   // workQueue
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );

        for (int i = 0; i < 8; i++) {
            final int id = i;
            executor.execute(() -> {
                System.out.printf("[%s] Görev-%d | Active=%d, Pool=%d, Queue=%d%n",
                        Thread.currentThread().getName(), id,
                        executor.getActiveCount(),
                        executor.getPoolSize(),
                        executor.getQueue().size());
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Tamamlanan görev: " + executor.getCompletedTaskCount());
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== EXECUTOR SERVICE ===");

        fixedThreadPoolDemo();
        futureDemo();
        invokeAllDemo();
        invokeAnyDemo();
        scheduledDemo();
        customThreadPoolDemo();
    }
}
