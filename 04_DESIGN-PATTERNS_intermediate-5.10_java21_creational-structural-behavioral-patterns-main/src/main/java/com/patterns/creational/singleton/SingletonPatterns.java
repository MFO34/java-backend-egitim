package com.patterns.creational.singleton;

import java.util.concurrent.atomic.AtomicReference;

/**
 * SINGLETON PATTERN — Tek örnek garantisi
 *
 * Kullanım: Logger, Config, ConnectionPool, Cache
 * Mülakat: "Thread-safe singleton nasıl yazılır?"
 */
public class SingletonPatterns {

    // ----------------------------------------------------------------
    // 1. Eager Initialization — class load edilince oluşturulur
    //    Avantaj: basit, thread-safe
    //    Dezavantaj: kullanılmasa bile bellek kullanır
    // ----------------------------------------------------------------
    static class EagerSingleton {
        private static final EagerSingleton INSTANCE = new EagerSingleton();
        private EagerSingleton() {}
        public static EagerSingleton getInstance() { return INSTANCE; }
    }

    // ----------------------------------------------------------------
    // 2. Lazy + Double-Checked Locking (en yaygın pattern)
    //    volatile zorunlu — instruction reordering'i önler
    // ----------------------------------------------------------------
    static class LazyDCLSingleton {
        private static volatile LazyDCLSingleton instance;
        private LazyDCLSingleton() {}

        public static LazyDCLSingleton getInstance() {
            if (instance == null) {                    // 1. kontrol (kilitsiz)
                synchronized (LazyDCLSingleton.class) {
                    if (instance == null) {            // 2. kontrol (kilitli)
                        instance = new LazyDCLSingleton();
                    }
                }
            }
            return instance;
        }
    }

    // ----------------------------------------------------------------
    // 3. Bill Pugh (Initialization-on-demand) — EN İYİ yaklaşım
    //    Lazy + thread-safe + reflection-safe (neredeyse)
    //    JVM class loading mekanizması thread-safety sağlar
    // ----------------------------------------------------------------
    static class BillPughSingleton {
        private BillPughSingleton() {}

        private static class Holder {
            private static final BillPughSingleton INSTANCE = new BillPughSingleton();
        }

        public static BillPughSingleton getInstance() { return Holder.INSTANCE; }
    }

    // ----------------------------------------------------------------
    // 4. Enum Singleton — reflection ve serialization safe
    //    Effective Java (Joshua Bloch) tavsiyesi
    // ----------------------------------------------------------------
    enum EnumSingleton {
        INSTANCE;
        public void doSomething() { System.out.println("Enum Singleton çalışıyor"); }
    }

    // ----------------------------------------------------------------
    // 5. Gerçek Dünya: ApplicationConfig Singleton
    // ----------------------------------------------------------------
    static class ApplicationConfig {
        private static volatile ApplicationConfig instance;
        private final String dbUrl;
        private final int maxConnections;

        private ApplicationConfig() {
            this.dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/app");
            this.maxConnections = Integer.parseInt(System.getenv().getOrDefault("MAX_CONN", "10"));
        }

        public static ApplicationConfig getInstance() {
            if (instance == null) {
                synchronized (ApplicationConfig.class) {
                    if (instance == null) instance = new ApplicationConfig();
                }
            }
            return instance;
        }

        public String getDbUrl() { return dbUrl; }
        public int getMaxConnections() { return maxConnections; }
    }

    public static void main(String[] args) {
        System.out.println("=== SINGLETON ===");

        // Aynı instance mi?
        System.out.println(EagerSingleton.getInstance() == EagerSingleton.getInstance());       // true
        System.out.println(LazyDCLSingleton.getInstance() == LazyDCLSingleton.getInstance());   // true
        System.out.println(BillPughSingleton.getInstance() == BillPughSingleton.getInstance()); // true

        EnumSingleton.INSTANCE.doSomething();

        ApplicationConfig config = ApplicationConfig.getInstance();
        System.out.println("DB: " + config.getDbUrl());
        System.out.println("MaxConn: " + config.getMaxConnections());
    }
}
