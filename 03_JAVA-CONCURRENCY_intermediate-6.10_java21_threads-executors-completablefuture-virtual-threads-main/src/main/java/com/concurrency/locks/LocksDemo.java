package com.concurrency.locks;

import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;
import java.util.*;

/**
 * LOCKS — ReentrantLock, ReadWriteLock, StampedLock
 *
 * synchronized vs ReentrantLock:
 *   synchronized      → basit, otomatik release
 *   ReentrantLock     → tryLock, timeout, fairness, interruptible
 *   ReadWriteLock     → çok okuyucu / tek yazıcı
 *   StampedLock       → optimistic read (en performanslı)
 */
public class LocksDemo {

    // ----------------------------------------------------------------
    // 1. ReentrantLock — synchronized'dan daha esnek
    // ----------------------------------------------------------------
    static class ReentrantLockDemo {
        private final ReentrantLock lock = new ReentrantLock(true); // fair=true
        private int sharedResource = 0;

        public void increment() {
            lock.lock();
            try {
                sharedResource++;
                System.out.printf("[%s] value=%d, holdCount=%d%n",
                        Thread.currentThread().getName(), sharedResource, lock.getHoldCount());
            } finally {
                lock.unlock(); // finally içinde mutlaka unlock!
            }
        }

        // tryLock — kilidi alamazsa beklemez
        public boolean tryIncrement() {
            if (lock.tryLock()) {
                try {
                    sharedResource++;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            System.out.println("[" + Thread.currentThread().getName() + "] Kilit alınamadı, geçildi.");
            return false;
        }

        // tryLock timeout ile
        public boolean tryIncrementWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
            if (lock.tryLock(timeout, unit)) {
                try {
                    sharedResource++;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            return false;
        }

        // Reentrant — aynı thread kilidi birden fazla alabilir
        public void reentrantMethod() {
            lock.lock();
            try {
                System.out.println("Dış metod, holdCount=" + lock.getHoldCount());
                innerMethod(); // aynı thread yeniden lock alabilir
            } finally {
                lock.unlock();
            }
        }

        private void innerMethod() {
            lock.lock();
            try {
                System.out.println("İç metod, holdCount=" + lock.getHoldCount());
            } finally {
                lock.unlock();
            }
        }

        public int getValue() { return sharedResource; }
    }

    // ----------------------------------------------------------------
    // 2. ReadWriteLock — Çok okuyucu / tek yazıcı
    // Okuma çok, yazma az olan senaryolarda performans kazancı
    // ----------------------------------------------------------------
    static class Cache {
        private final Map<String, String> data = new HashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock  = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();

        public String get(String key) {
            readLock.lock(); // Birden fazla thread eş zamanlı okuyabilir
            try {
                Thread.sleep(10); // okuma simülasyonu
                return data.get(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                readLock.unlock();
            }
        }

        public void put(String key, String value) {
            writeLock.lock(); // Yalnızca bir thread yazabilir, okumalar bloklanır
            try {
                Thread.sleep(50); // yazma simülasyonu
                data.put(key, value);
                System.out.printf("[%s] Yazıldı: %s=%s%n",
                        Thread.currentThread().getName(), key, value);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writeLock.unlock();
            }
        }
    }

    // ----------------------------------------------------------------
    // 3. StampedLock — Optimistic Read (Java 8+)
    // En performanslı seçenek — yazma nadir, okuma çok yoğunsa ideal
    // ----------------------------------------------------------------
    static class Point {
        private double x, y;
        private final StampedLock sl = new StampedLock();

        public void move(double dx, double dy) {
            long stamp = sl.writeLock();
            try { x += dx; y += dy; }
            finally { sl.unlockWrite(stamp); }
        }

        public double distanceFromOrigin() {
            long stamp = sl.tryOptimisticRead(); // kilitsiz okuma denemesi
            double cx = x, cy = y;
            if (!sl.validate(stamp)) { // bu sürede yazma oldu mu?
                stamp = sl.readLock(); // evet → gerçek read lock al
                try { cx = x; cy = y; }
                finally { sl.unlockRead(stamp); }
            }
            return Math.sqrt(cx * cx + cy * cy);
        }
    }

    // ----------------------------------------------------------------
    // 4. Condition — wait/notify'nin lock versiyonu
    // ----------------------------------------------------------------
    static class BoundedBuffer<T> {
        private final List<T> buffer = new LinkedList<>();
        private final int maxSize;
        private final Lock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        BoundedBuffer(int maxSize) { this.maxSize = maxSize; }

        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (buffer.size() == maxSize) notFull.await(); // doluysa bekle
                buffer.add(item);
                System.out.printf("[%s] Put: %s (size=%d)%n",
                        Thread.currentThread().getName(), item, buffer.size());
                notEmpty.signalAll(); // boş değil, tüketicileri uyandır
            } finally { lock.unlock(); }
        }

        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (buffer.isEmpty()) notEmpty.await(); // boşsa bekle
                T item = ((LinkedList<T>) buffer).removeFirst();
                System.out.printf("[%s] Take: %s (size=%d)%n",
                        Thread.currentThread().getName(), item, buffer.size());
                notFull.signalAll(); // dolu değil, üreticileri uyandır
                return item;
            } finally { lock.unlock(); }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== LOCKS ===\n");

        // ReentrantLock
        ReentrantLockDemo rld = new ReentrantLockDemo();
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i;
            threads[i] = new Thread(() -> rld.increment(), "T-" + id);
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        System.out.println("ReentrantLock sonuç: " + rld.getValue());

        rld.reentrantMethod();

        // ReadWriteLock Cache
        Cache cache = new Cache();
        Thread writer = new Thread(() -> {
            cache.put("java", "21");
            cache.put("spring", "3.2");
        }, "Writer");
        writer.start();
        writer.join();

        Thread[] readers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            readers[i] = new Thread(() ->
                    System.out.printf("[%s] java=%s%n", Thread.currentThread().getName(), cache.get("java")),
                    "Reader-" + i);
            readers[i].start();
        }
        for (Thread r : readers) r.join();

        // BoundedBuffer — Üretici/Tüketici
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) { buffer.put(i); Thread.sleep(50); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "Producer");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) { buffer.take(); Thread.sleep(100); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "Consumer");
        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }
}
