package com.concurrency.collections;

import java.util.*;
import java.util.concurrent.*;

/**
 * CONCURRENT COLLECTIONS — Thread-safe veri yapıları
 *
 * Neden Collections.synchronizedXxx() yerine concurrent sınıflar?
 *   synchronizedMap → tüm metodlar synchronized, düşük performans
 *   ConcurrentHashMap → segment/bucket bazlı kilit, yüksek performans
 *
 * Kılavuz:
 *   ConcurrentHashMap    → yüksek trafikli cache, sayaçlar
 *   CopyOnWriteArrayList → okuma ağır, yazma nadir listeler
 *   BlockingQueue        → producer-consumer
 *   ConcurrentLinkedQueue→ lock-free queue
 */
public class ConcurrentCollectionsDemo {

    // ----------------------------------------------------------------
    // 1. ConcurrentHashMap
    // ----------------------------------------------------------------
    static void concurrentHashMapDemo() throws InterruptedException {
        System.out.println("\n--- ConcurrentHashMap ---");
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        Thread[] writers = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i;
            writers[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    map.merge("key-" + (id % 3), 1, Integer::sum); // atomik güncelleme
                }
            });
            writers[i].start();
        }
        for (Thread t : writers) t.join();

        System.out.println("Map boyutu: " + map.size());
        System.out.println("Toplam değer: " + map.values().stream().mapToInt(v -> v).sum());

        // computeIfAbsent — yalnızca yoksa hesapla (lazy initialization)
        map.computeIfAbsent("new-key", k -> k.length() * 10);

        // putIfAbsent — yalnızca yoksa ekle
        map.putIfAbsent("new-key", 999);
        System.out.println("new-key: " + map.get("new-key")); // 80, 999 değil
    }

    // ----------------------------------------------------------------
    // 2. CopyOnWriteArrayList — Okuma kilitsiz, yazma kopyalama
    // ----------------------------------------------------------------
    static void copyOnWriteDemo() throws InterruptedException {
        System.out.println("\n--- CopyOnWriteArrayList ---");
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.addAll(List.of("A", "B", "C"));

        // Okuma sırasında listeyi güvenle iterate edebilirsin
        Thread reader = new Thread(() -> {
            for (String s : list) { // ConcurrentModificationException olmaz
                System.out.print(s + " ");
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            System.out.println();
        });

        Thread writer = new Thread(() -> {
            try { Thread.sleep(75); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            list.add("D"); // okuma snapshot'ı etkilenemez
            System.out.println("D eklendi");
        });

        reader.start(); writer.start();
        reader.join(); writer.join();
        System.out.println("Final liste: " + list);
    }

    // ----------------------------------------------------------------
    // 3. BlockingQueue — Producer-Consumer
    // ----------------------------------------------------------------
    static void blockingQueueDemo() throws InterruptedException {
        System.out.println("\n--- BlockingQueue (Producer-Consumer) ---");
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(5);

        Thread producer = new Thread(() -> {
            String[] items = {"Sipariş-1", "Sipariş-2", "Sipariş-3", "Sipariş-4", "Sipariş-5"};
            for (String item : items) {
                try {
                    queue.put(item); // dolu ise bekle
                    System.out.println("[Producer] Eklendi: " + item + " | Queue: " + queue.size());
                    Thread.sleep(100);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(200);
                    String item = queue.take(); // boş ise bekle
                    System.out.println("[Consumer] Alındı: " + item + " | Queue: " + queue.size());
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }

    // ----------------------------------------------------------------
    // 4. PriorityBlockingQueue — Öncelikli kuyruk
    // ----------------------------------------------------------------
    static void priorityBlockingQueueDemo() throws InterruptedException {
        System.out.println("\n--- PriorityBlockingQueue ---");
        PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();
        queue.put(new Task("Düşük",  3));
        queue.put(new Task("Yüksek", 1));
        queue.put(new Task("Orta",   2));

        while (!queue.isEmpty()) System.out.println("İşleniyor: " + queue.take());
    }

    static class Task implements Comparable<Task> {
        String name; int priority;
        Task(String name, int priority) { this.name = name; this.priority = priority; }

        @Override
        public int compareTo(Task other) { return Integer.compare(this.priority, other.priority); }

        @Override
        public String toString() { return name + "(öncelik=" + priority + ")"; }
    }

    // ----------------------------------------------------------------
    // 5. CountDownLatch — N görev bitene kadar bekle
    // ----------------------------------------------------------------
    static void countDownLatchDemo() throws InterruptedException {
        System.out.println("\n--- CountDownLatch ---");
        int serviceCount = 3;
        CountDownLatch latch = new CountDownLatch(serviceCount);

        for (int i = 1; i <= serviceCount; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    Thread.sleep(id * 200L);
                    System.out.println("Servis-" + id + " hazır");
                    latch.countDown();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
        }

        System.out.println("Tüm servislerin hazır olması bekleniyor...");
        latch.await(); // count 0 olana kadar bekle
        System.out.println("Tüm servisler hazır, uygulama başlatılıyor!");
    }

    // ----------------------------------------------------------------
    // 6. CyclicBarrier — Thread'leri checkpoint'te buluştur
    // ----------------------------------------------------------------
    static void cyclicBarrierDemo() throws InterruptedException {
        System.out.println("\n--- CyclicBarrier ---");
        int threadCount = 3;
        CyclicBarrier barrier = new CyclicBarrier(threadCount,
                () -> System.out.println("=== Tüm thread'ler checkpoint'e ulaştı ==="));

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    System.out.println("Thread-" + id + " hazırlık yapıyor...");
                    Thread.sleep((id + 1) * 100L);
                    System.out.println("Thread-" + id + " checkpoint'e geldi");
                    barrier.await(); // herkes gelene kadar bekle
                    System.out.println("Thread-" + id + " devam ediyor");
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        Thread.sleep(1000);
    }

    // ----------------------------------------------------------------
    // 7. Semaphore — Kaynak erişimini sınırla
    // ----------------------------------------------------------------
    static void semaphoreDemo() throws InterruptedException {
        System.out.println("\n--- Semaphore (Connection Pool simülasyonu) ---");
        Semaphore semaphore = new Semaphore(3); // max 3 eş zamanlı bağlantı

        Thread[] threads = new Thread[7];
        for (int i = 0; i < 7; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.printf("[Thread-%d] Bağlantı alındı (mevcut permit: %d)%n",
                            id, semaphore.availablePermits());
                    Thread.sleep(300);
                    System.out.println("[Thread-" + id + "] Bağlantı bırakıldı");
                    semaphore.release();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== CONCURRENT COLLECTIONS ===");
        concurrentHashMapDemo();
        copyOnWriteDemo();
        blockingQueueDemo();
        priorityBlockingQueueDemo();
        countDownLatchDemo();
        cyclicBarrierDemo();
        semaphoreDemo();
    }
}
