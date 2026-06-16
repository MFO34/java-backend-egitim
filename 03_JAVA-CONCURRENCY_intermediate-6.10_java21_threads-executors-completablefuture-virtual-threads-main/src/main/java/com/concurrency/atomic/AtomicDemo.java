package com.concurrency.atomic;

import java.util.concurrent.atomic.*;

/**
 * ATOMIC CLASSES — Lock-free Thread Safety
 *
 * synchronized yerine atomic sınıflar:
 *   - Daha performanslı (CAS — Compare and Swap)
 *   - Lock olmadığı için deadlock riski yok
 *   - CPU seviyesinde atomik operasyonlar
 *
 * CAS (Compare and Swap):
 *   Beklenen değeri kontrol et, eşleşiyorsa yeni değeri yaz.
 *   Eşleşmiyorsa tekrar dene.
 */
public class AtomicDemo {

    // ----------------------------------------------------------------
    // 1. AtomicInteger — Thread-safe sayaç
    // ----------------------------------------------------------------
    static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public int increment()          { return count.incrementAndGet(); }
        public int decrement()          { return count.decrementAndGet(); }
        public int addAndGet(int delta) { return count.addAndGet(delta); }
        public int get()                { return count.get(); }

        // CAS — sadece beklenen değerse güncelle
        public boolean compareAndSet(int expected, int newValue) {
            return count.compareAndSet(expected, newValue);
        }
    }

    // ----------------------------------------------------------------
    // 2. AtomicLong — ID üretici
    // ----------------------------------------------------------------
    static class IdGenerator {
        private static final AtomicLong sequence = new AtomicLong(1000);

        public static long nextId() { return sequence.incrementAndGet(); }
    }

    // ----------------------------------------------------------------
    // 3. AtomicReference — Referans güncelleme
    // ----------------------------------------------------------------
    static class AtomicStack<T> {
        private static class Node<T> {
            final T val;
            final Node<T> next;
            Node(T val, Node<T> next) { this.val = val; this.next = next; }
        }

        private final AtomicReference<Node<T>> head = new AtomicReference<>();

        public void push(T val) {
            while (true) {
                Node<T> current = head.get();
                Node<T> newHead = new Node<>(val, current);
                if (head.compareAndSet(current, newHead)) return; // başarılı → çık
                // başarısız → başka thread değiştirdi, tekrar dene
            }
        }

        public T pop() {
            while (true) {
                Node<T> current = head.get();
                if (current == null) return null;
                if (head.compareAndSet(current, current.next)) return current.val;
            }
        }
    }

    // ----------------------------------------------------------------
    // 4. AtomicBoolean — Flag
    // ----------------------------------------------------------------
    static class OneTimeAction {
        private final AtomicBoolean executed = new AtomicBoolean(false);

        public void execute(Runnable action) {
            if (executed.compareAndSet(false, true)) {
                action.run();
            } else {
                System.out.println("Zaten çalıştırıldı, atlanıyor.");
            }
        }
    }

    // ----------------------------------------------------------------
    // 5. LongAdder — Yüksek contention'da AtomicLong'dan hızlı
    //    Birden fazla thread'in ayrı cell'lere yazması, sonunda toplanır
    // ----------------------------------------------------------------
    static class PerformanceComparison {
        private final AtomicLong atomicLong = new AtomicLong(0);
        private final LongAdder  longAdder  = new LongAdder();

        public void benchmark(int threadCount, int incrementsPerThread) throws InterruptedException {
            Thread[] threads = new Thread[threadCount];

            // AtomicLong
            long start = System.nanoTime();
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) atomicLong.incrementAndGet();
                });
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            long atomicTime = System.nanoTime() - start;

            // LongAdder
            start = System.nanoTime();
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) longAdder.increment();
                });
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            long adderTime = System.nanoTime() - start;

            System.out.printf("AtomicLong: %dms | LongAdder: %dms%n",
                    atomicTime / 1_000_000, adderTime / 1_000_000);
            System.out.printf("AtomicLong sonuç: %d | LongAdder sonuç: %d%n",
                    atomicLong.get(), longAdder.sum());
        }
    }

    // ----------------------------------------------------------------
    // 6. ABA Problemi
    // A → B → A değişiminde CAS yanıltılabilir
    // Çözüm: AtomicStampedReference (versiyon numarası ile)
    // ----------------------------------------------------------------
    static void abaDemo() {
        AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(1, 0);

        int[] stamp = new int[1];
        int val = ref.get(stamp); // değer ve stamp'i al
        System.out.println("Başlangıç: val=" + val + ", stamp=" + stamp[0]);

        // Başka thread A→B→A yaptı, ama stamp farklı olduğu için CAS başarısız
        ref.compareAndSet(1, 2, 0, 1); // A→B (stamp 0→1)
        ref.compareAndSet(2, 1, 1, 2); // B→A (stamp 1→2)

        boolean success = ref.compareAndSet(1, 99, 0, 1); // eski stamp ile deneme
        System.out.println("ABA sonrası CAS başarılı mı: " + success); // false
        System.out.println("Güncel: val=" + ref.getReference() + ", stamp=" + ref.getStamp());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ATOMIC CLASSES ===\n");

        // AtomicCounter
        AtomicCounter counter = new AtomicCounter();
        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++)
            threads[i] = new Thread(counter::increment);
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        System.out.println("Atomic counter (beklenen 100): " + counter.get());

        // CAS
        System.out.println("CAS(0→99): " + counter.compareAndSet(0, 99));
        System.out.println("CAS(100→99): " + counter.compareAndSet(100, 99));

        // IdGenerator
        System.out.println("ID-1: " + IdGenerator.nextId());
        System.out.println("ID-2: " + IdGenerator.nextId());

        // AtomicStack
        AtomicStack<String> stack = new AtomicStack<>();
        stack.push("A"); stack.push("B"); stack.push("C");
        System.out.println("Pop: " + stack.pop());
        System.out.println("Pop: " + stack.pop());

        // OneTimeAction
        OneTimeAction action = new OneTimeAction();
        action.execute(() -> System.out.println("İlk çalışma"));
        action.execute(() -> System.out.println("İkinci çalışma (çalışmayacak)"));

        // ABA Problemi
        System.out.println("\n--- ABA Problemi ---");
        abaDemo();

        // Performans karşılaştırması
        System.out.println("\n--- LongAdder vs AtomicLong ---");
        new PerformanceComparison().benchmark(10, 100_000);
    }
}
