package com.ogrenci.util;

import com.ogrenci.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * SINGLETON PATTERN + HikariCP CONNECTION POOL
 *
 * SINGLETON PATTERN:
 * Uygulama boyunca sadece BİR HikariDataSource nesnesi olmasını sağlar.
 * HikariDataSource ağır bir nesnedir — her seferinde oluşturmak çok masraflı.
 *
 * Thread-safe Singleton yapısı:
 *   - private static volatile instance: tüm thread'ler aynı nesneyi görür
 *   - synchronized blok: aynı anda sadece bir thread oluşturabilir
 *   - Double-checked locking: performans için iki kez kontrol
 *
 * HIKARICP:
 * Connection Pool = bağlantı havuzu
 * - Uygulama açılışında N bağlantı açar (N = minimumIdle)
 * - Bir sorgu gelince havuzdan bağlantı verir
 * - Sorgu bitince bağlantıyı havuza iade eder
 * - Yeni bağlantı açmak yerine hazır bağlantı kullanır → çok hızlı
 */
public class DatabaseConnection {

    // volatile: RAM'e değil, doğrudan ana belleğe yaz (thread safety)
    // static: tüm sınıf için tek nesne
    private static volatile DatabaseConnection instance;

    // HikariDataSource: bağlantı havuzunu yöneten ana nesne
    private final HikariDataSource dataSource;

    // private constructor: dışarıdan new DatabaseConnection() denemesini engeller
    private DatabaseConnection() {
        // hikari.properties dosyasını classpath'ten yükle
        HikariConfig config = new HikariConfig("/hikari.properties");

        // HikariDataSource oluştur — bu sırada bağlantı havuzu başlatılır
        this.dataSource = new HikariDataSource(config);

        System.out.println("Veritabanı bağlantı havuzu başlatıldı.");
    }

    /**
     * DOUBLE-CHECKED LOCKING Singleton:
     *
     * if (instance == null)             → İlk kontrol: nesne var mı?
     *   synchronized(DatabaseConnection.class) → Kilitli bölge
     *     if (instance == null)         → İkinci kontrol: başka thread oluşturduysa?
     *       instance = new ...()        → Sadece bir kez oluştur
     *
     * synchronized: aynı anda sadece bir thread bu bloğa girebilir
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {                           // İlk hızlı kontrol
            synchronized (DatabaseConnection.class) {    // Kilitle
                if (instance == null) {                   // İkinci güvenli kontrol
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Havuzdan bağlantı al.
     *
     * JDBC BAĞLANTI DÖNGÜSÜ:
     * 1. getConnection() → havuzdan bağlantı al
     * 2. SQL çalıştır
     * 3. connection.close() → bağlantıyı HAVUZA İADE ET (gerçekten kapatmaz!)
     *
     * try-with-resources otomatik close() çağırır.
     */
    public Connection getConnection() {
        try {
            // HikariCP havuzdan bir bağlantı döner (~1ms)
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException(
                "Veritabanı bağlantısı alınamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Uygulama kapanırken bağlantı havuzunu kapat.
     * Tüm bağlantılar kapatılır, kaynaklar serbest bırakılır.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Bağlantı havuzu kapatıldı.");
        }
    }

    /** Pool durumu hakkında bilgi ver */
    public void printPoolStatus() {
        var pool = dataSource.getHikariPoolMXBean();
        System.out.printf(
            "HikariCP: Aktif=%d, Boşta=%d, Bekleyen=%d, Toplam=%d%n",
            pool.getActiveConnections(),
            pool.getIdleConnections(),
            pool.getThreadsAwaitingConnection(),
            pool.getTotalConnections()
        );
    }
}
