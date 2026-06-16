package com.concurrency.patterns;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PRODUCER-CONSUMER PATTERN
 *
 * En klasik concurrency problemi.
 * Üreticiler ve tüketiciler arasında güvenli veri aktarımı.
 *
 * Gerçek dünya kullanımı:
 *   - Kafka consumer + işlemci
 *   - HTTP istek kuyruğu
 *   - Log yazıcı
 *   - Batch işlemci
 */
public class ProducerConsumer {

    record Order(int id, String product, double price) {}

    static class OrderProducer implements Runnable {
        private final BlockingQueue<Order> queue;
        private final int count;
        private final AtomicInteger idGen;

        OrderProducer(BlockingQueue<Order> queue, int count, AtomicInteger idGen) {
            this.queue = queue;
            this.count = count;
            this.idGen = idGen;
        }

        @Override
        public void run() {
            String[] products = {"Laptop", "Phone", "Tablet", "Watch"};
            for (int i = 0; i < count; i++) {
                try {
                    Order order = new Order(
                            idGen.incrementAndGet(),
                            products[i % products.length],
                            Math.random() * 1000
                    );
                    queue.put(order);
                    System.out.printf("[%s] Üretildi: %s%n",
                            Thread.currentThread().getName(), order);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    static class OrderConsumer implements Runnable {
        private final BlockingQueue<Order> queue;
        private final CountDownLatch latch;
        private final int expectedCount;
        private int processed = 0;

        OrderConsumer(BlockingQueue<Order> queue, CountDownLatch latch, int expectedCount) {
            this.queue = queue;
            this.latch = latch;
            this.expectedCount = expectedCount;
        }

        @Override
        public void run() {
            while (processed < expectedCount) {
                try {
                    Order order = queue.poll(500, TimeUnit.MILLISECONDS);
                    if (order != null) {
                        System.out.printf("[%s] İşlendi: %s%n",
                                Thread.currentThread().getName(), order);
                        processed++;
                        latch.countDown();
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== PRODUCER-CONSUMER PATTERN ===\n");

        int totalOrders = 10;
        BlockingQueue<Order> queue = new LinkedBlockingQueue<>(5);
        CountDownLatch latch = new CountDownLatch(totalOrders);
        AtomicInteger idGen = new AtomicInteger(0);

        // 2 producer, 3 consumer
        ExecutorService producers = Executors.newFixedThreadPool(2);
        ExecutorService consumers = Executors.newFixedThreadPool(3);

        producers.submit(new OrderProducer(queue, 5, idGen));
        producers.submit(new OrderProducer(queue, 5, idGen));

        for (int i = 0; i < 3; i++)
            consumers.submit(new OrderConsumer(queue, latch, 4));

        latch.await(10, TimeUnit.SECONDS);

        producers.shutdown();
        consumers.shutdown();

        System.out.println("\nToplam işlenen sipariş: " + (totalOrders - latch.getCount()));
    }
}
