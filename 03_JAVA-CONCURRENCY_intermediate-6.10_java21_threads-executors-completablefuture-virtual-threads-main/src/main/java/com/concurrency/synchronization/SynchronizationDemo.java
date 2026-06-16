package com.concurrency.synchronization;

/**
 * SYNCHRONIZATION — Race Condition, synchronized, volatile
 *
 * Race Condition: Birden fazla thread aynı veriye eş zamanlı eriştiğinde
 * sonucun thread çalışma sırasına bağlı olması.
 *
 * Çözümler:
 *   1. synchronized keyword
 *   2. volatile keyword
 *   3. Atomic sınıflar
 *   4. Lock nesneleri
 */
public class SynchronizationDemo {

    // ----------------------------------------------------------------
    // 1. Race Condition — YANLIŞ implementasyon
    // ----------------------------------------------------------------
    static class UnsafeCounter {
        private int count = 0;

        public void increment() { count++; } // READ → MODIFY → WRITE (atomik değil!)
        public int getCount() { return count; }
    }

    // ----------------------------------------------------------------
    // 2. synchronized method — DOĞRU implementasyon
    // Bir seferde yalnızca bir thread çalışabilir
    // ----------------------------------------------------------------
    static class SynchronizedCounter {
        private int count = 0;

        public synchronized void increment() { count++; }
        public synchronized void decrement() { count--; }
        public synchronized int getCount() { return count; }
    }

    // ----------------------------------------------------------------
    // 3. synchronized block — daha ince granüllü kilit
    // Sınıfın tamamını değil, sadece kritik bölümü kilitler
    // ----------------------------------------------------------------
    static class BankAccount {
        private double balance;
        private final Object lock = new Object(); // dedicated lock object

        BankAccount(double initialBalance) { this.balance = initialBalance; }

        public void deposit(double amount) {
            synchronized (lock) {
                if (amount <= 0) throw new IllegalArgumentException("Geçersiz miktar");
                balance += amount;
                System.out.printf("[%s] Para yatırıldı: %.0f → Bakiye: %.0f%n",
                        Thread.currentThread().getName(), amount, balance);
            }
        }

        public void withdraw(double amount) {
            synchronized (lock) {
                if (amount > balance) throw new IllegalStateException("Yetersiz bakiye");
                balance -= amount;
                System.out.printf("[%s] Para çekildi: %.0f → Bakiye: %.0f%n",
                        Thread.currentThread().getName(), amount, balance);
            }
        }

        public double getBalance() {
            synchronized (lock) { return balance; }
        }
    }

    // ----------------------------------------------------------------
    // 4. volatile — görünürlük garantisi (atomiklik değil)
    // Bir thread'in yaptığı değişiklik diğer thread'lere hemen görünür
    // ----------------------------------------------------------------
    static class StatusFlag {
        private volatile boolean running = true; // cache'lenmez, her zaman main memory'den okunur

        public void stop() { running = false; }

        public void run() {
            while (running) {
                // running değeri volatile olmazsa thread cache'lenmiş eski değeri okuyabilir
                // ve döngü hiç çıkmaz (infinite loop)
            }
            System.out.println("[" + Thread.currentThread().getName() + "] Durdu.");
        }
    }

    // ----------------------------------------------------------------
    // 5. wait() / notify() — Thread iletişimi
    // ----------------------------------------------------------------
    static class MessageQueue {
        private String message;
        private boolean hasMessage = false;

        public synchronized void produce(String msg) throws InterruptedException {
            while (hasMessage) wait(); // tüketicinin almasını bekle
            this.message = msg;
            hasMessage = true;
            System.out.println("[Producer] Mesaj gönderildi: " + msg);
            notifyAll(); // bekleyen tüketicileri uyandır
        }

        public synchronized String consume() throws InterruptedException {
            while (!hasMessage) wait(); // üreticinin mesaj göndermesini bekle
            hasMessage = false;
            System.out.println("[Consumer] Mesaj alındı: " + message);
            notifyAll(); // bekleyen üreticileri uyandır
            return message;
        }
    }

    // ----------------------------------------------------------------
    // 6. Deadlock örneği ve önleme
    // ----------------------------------------------------------------
    static class DeadlockExample {
        private final Object lockA = new Object();
        private final Object lockB = new Object();

        // DEADLOCK: Thread1 lockA'yı alır, Thread2 lockB'yi alır
        // Thread1 lockB'yi bekler, Thread2 lockA'yı bekler → kilitlenir
        public void deadlockMethod1() {
            synchronized (lockA) {
                System.out.println("Thread1 lockA aldı");
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                synchronized (lockB) { // Thread2 lockB'yi tutuyor → DEADLOCK
                    System.out.println("Thread1 lockB aldı");
                }
            }
        }

        public void deadlockMethod2() {
            synchronized (lockB) {
                System.out.println("Thread2 lockB aldı");
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                synchronized (lockA) { // Thread1 lockA'yı tutuyor → DEADLOCK
                    System.out.println("Thread2 lockA aldı");
                }
            }
        }

        // ÇÖZÜM: Kilitleri her zaman aynı sırayla al
        public void safeMethod1() {
            synchronized (lockA) {      // her zaman önce lockA
                synchronized (lockB) {  // sonra lockB
                    System.out.println("Thread1 güvenli çalıştı");
                }
            }
        }

        public void safeMethod2() {
            synchronized (lockA) {      // her zaman önce lockA
                synchronized (lockB) {  // sonra lockB
                    System.out.println("Thread2 güvenli çalıştı");
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SYNCHRONIZATION ===\n");

        // Race Condition gösterimi
        UnsafeCounter unsafe = new UnsafeCounter();
        SynchronizedCounter safe = new SynchronizedCounter();

        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
                unsafe.increment();
                safe.increment();
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();

        System.out.println("Unsafe count (beklenen 100): " + unsafe.getCount());
        System.out.println("Safe count   (beklenen 100): " + safe.getCount());

        // BankAccount
        BankAccount account = new BankAccount(1000);
        Thread t1 = new Thread(() -> account.deposit(500), "T1-Deposit");
        Thread t2 = new Thread(() -> account.withdraw(200), "T2-Withdraw");
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println("Final bakiye: " + account.getBalance());

        // wait/notify
        MessageQueue queue = new MessageQueue();
        Thread producer = new Thread(() -> {
            try {
                queue.produce("Java Concurrency!");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try {
                queue.consume();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        consumer.start(); producer.start();
        consumer.join(); producer.join();
    }
}
