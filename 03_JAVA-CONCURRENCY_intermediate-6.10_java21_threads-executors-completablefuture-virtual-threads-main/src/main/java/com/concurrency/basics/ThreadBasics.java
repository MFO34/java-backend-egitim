package com.concurrency.basics;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * THREAD TEMELLERİ
 *
 * Thread Lifecycle:
 *   NEW → RUNNABLE → RUNNING → BLOCKED/WAITING/TIMED_WAITING → TERMINATED
 *
 * Thread oluşturmanın 3 yolu:
 *   1. Thread sınıfını extend etmek
 *   2. Runnable implement etmek (tercih edilen)
 *   3. Callable + FutureTask (dönüş değeri + exception için)
 */
public class ThreadBasics {

    // ----------------------------------------------------------------
    // 1. Thread'i extend etmek (önerilmez — Java tek kalıtım)
    // ----------------------------------------------------------------
    static class MyThread extends Thread {
        private final String taskName;

        MyThread(String taskName) { this.taskName = taskName; }

        @Override
        public void run() {
            System.out.printf("[%s] Thread ID=%d, Name=%s%n",
                    taskName, Thread.currentThread().threadId(), Thread.currentThread().getName());
        }
    }

    // ----------------------------------------------------------------
    // 2. Runnable implement etmek (tercih edilen)
    // ----------------------------------------------------------------
    static class PrintTask implements Runnable {
        private final String message;
        private final int repeat;

        PrintTask(String message, int repeat) {
            this.message = message;
            this.repeat = repeat;
        }

        @Override
        public void run() {
            for (int i = 0; i < repeat; i++) {
                System.out.printf("[%s] %s - %d%n", Thread.currentThread().getName(), message, i);
                try { Thread.sleep(100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // interrupt flag'i geri set et
                    return;
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // 3. Callable — sonuç döndürür ve exception fırlatabilir
    // ----------------------------------------------------------------
    static class SumCallable implements Callable<Long> {
        private final int start, end;

        SumCallable(int start, int end) { this.start = start; this.end = end; }

        @Override
        public Long call() {
            long sum = 0;
            for (int i = start; i <= end; i++) sum += i;
            System.out.printf("[%s] Sum %d-%d = %d%n", Thread.currentThread().getName(), start, end, sum);
            return sum;
        }
    }

    // ----------------------------------------------------------------
    // Thread Durumları ve Metodları
    // ----------------------------------------------------------------
    static class ThreadStateDemo {

        public void demonstrateJoin() throws InterruptedException {
            Thread worker = new Thread(() -> {
                System.out.println("Worker başladı...");
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("Worker bitti.");
            });

            worker.start();
            System.out.println("Worker durumu: " + worker.getState()); // RUNNABLE
            worker.join(); // worker bitene kadar bekle
            System.out.println("Worker durumu join sonrası: " + worker.getState()); // TERMINATED
        }

        public void demonstrateInterrupt() {
            Thread worker = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Çalışıyorum...");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupt sinyali alındı, duruyorum.");
                        Thread.currentThread().interrupt(); // flag'i geri set et
                        return;
                    }
                }
            });

            worker.start();
            try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            worker.interrupt(); // dışarıdan durdur
        }

        public void demonstrateDaemon() {
            Thread daemon = new Thread(() -> {
                while (true) {
                    System.out.println("Daemon çalışıyor...");
                    try { Thread.sleep(100); } catch (InterruptedException e) { return; }
                }
            });
            daemon.setDaemon(true); // JVM daemon thread beklemeden kapanır
            daemon.start();
        }

        public void demonstratePriority() {
            Thread low  = new Thread(() -> System.out.println("Düşük öncelik"));
            Thread high = new Thread(() -> System.out.println("Yüksek öncelik"));
            low.setPriority(Thread.MIN_PRIORITY);   // 1
            high.setPriority(Thread.MAX_PRIORITY);  // 10
            low.start();
            high.start();
        }
    }

    // ----------------------------------------------------------------
    // Thread-Local — Her thread'in kendi kopyası
    // ----------------------------------------------------------------
    static class ThreadLocalDemo {
        // Her thread kendi userId değerini saklar (request context için ideal)
        private static final ThreadLocal<String> userContext = new ThreadLocal<>();

        public void processRequest(String userId) {
            userContext.set(userId);
            try {
                System.out.printf("[%s] İşlem başladı: %s%n",
                        Thread.currentThread().getName(), userContext.get());
                Thread.sleep(100);
                System.out.printf("[%s] İşlem bitti: %s%n",
                        Thread.currentThread().getName(), userContext.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                userContext.remove(); // bellek sızıntısı önle
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== THREAD TEMELLERI ===\n");

        // 1. Thread extend
        new MyThread("Task-A").start();

        // 2. Runnable
        Thread t = new Thread(new PrintTask("Merhaba", 3), "Worker-1");
        t.start();

        // 3. Callable + FutureTask
        FutureTask<Long> futureTask = new FutureTask<>(new SumCallable(1, 100));
        new Thread(futureTask, "Sum-Thread").start();
        System.out.println("Callable sonuç: " + futureTask.get()); // blocking

        // Join
        ThreadStateDemo demo = new ThreadStateDemo();
        demo.demonstrateJoin();

        // Interrupt
        demo.demonstrateInterrupt();

        // ThreadLocal
        ThreadLocalDemo tlDemo = new ThreadLocalDemo();
        Thread u1 = new Thread(() -> tlDemo.processRequest("user-1"));
        Thread u2 = new Thread(() -> tlDemo.processRequest("user-2"));
        u1.start(); u2.start();
        u1.join(); u2.join();
    }
}
