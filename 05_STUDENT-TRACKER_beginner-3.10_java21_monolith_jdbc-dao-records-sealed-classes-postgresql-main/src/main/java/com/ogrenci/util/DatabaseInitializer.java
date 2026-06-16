package com.ogrenci.util;

import com.ogrenci.exception.DatabaseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Veritabanı tablolarını oluşturur ve örnek verilerle doldurur.
 *
 * ÖĞRENILEN KAVRAMLAR:
 *   - Classpath'ten kaynak dosya okuma (getResourceAsStream)
 *   - Statement ile çok satırlı SQL çalıştırma
 *   - Transaction ile atomik başlatma
 */
public class DatabaseInitializer {

    private final DatabaseConnection dbConnection;

    public DatabaseInitializer(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Şema ve örnek verileri yükle.
     * TRANSACTION: ya ikisi de başarılı olur, ya da ikisi de geri alınır.
     */
    public void initialize() {
        System.out.println("Veritabanı başlatılıyor...");

        // try-with-resources: Connection otomatik havuza iade edilir
        try (Connection conn = dbConnection.getConnection()) {

            // AUTO COMMIT'İ KAPAT: manuel transaction yönetimi
            // Varsayılan: her SQL ayrı transaction (autoCommit=true)
            // false yapınca: commit() çağırana kadar değişiklikler geçici
            conn.setAutoCommit(false);

            try {
                // 1. Şemayı oluştur (CREATE TABLE'lar)
                executeSqlFile(conn, "/schema.sql");
                System.out.println("  Tablolar oluşturuldu.");

                // 2. Örnek verileri ekle
                executeSqlFile(conn, "/data.sql");
                System.out.println("  Örnek veriler yüklendi.");

                // 3. COMMIT: Her şey başarılıysa kaydet
                conn.commit();
                System.out.println("Veritabanı başarıyla başlatıldı!");

            } catch (Exception e) {
                // ROLLBACK: Hata olursa tüm değişiklikleri geri al
                conn.rollback();
                System.err.println("Başlatma hatası, geri alındı: " + e.getMessage());
                throw new DatabaseException("Veritabanı başlatılamadı", e);
            } finally {
                // Her durumda autoCommit'i geri aç
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Bağlantı hatası", e);
        }
    }

    /**
     * SQL dosyasını classpath'ten oku ve çalıştır.
     *
     * getResourceAsStream: JAR içindeki /resources klasöründen okur
     * Statement (PreparedStatement değil): çok satırlı SQL için uygundur
     */
    private void executeSqlFile(Connection conn, String filePath) throws SQLException {
        // Classpath'ten dosyayı stream olarak aç
        InputStream is = getClass().getResourceAsStream(filePath);

        if (is == null) {
            throw new DatabaseException("SQL dosyası bulunamadı: " + filePath);
        }

        // Stream'i String'e çevir
        String sql;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            // Stream API ile satırları birleştir
            sql = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new DatabaseException("SQL dosyası okunamadı: " + filePath, e);
        }

        // Statement: dinamik SQL çalıştırmak için (SQL injection riski yok çünkü sabit dosya)
        try (Statement stmt = conn.createStatement()) {
            // execute(): SELECT dışı komutlar için (CREATE, INSERT, UPDATE, DELETE)
            stmt.execute(sql);
        }
    }

    /**
     * Sadece şemayı yeniden oluştur (verileri silmeden).
     * Geliştirme sırasında tablo yapısı değiştiyse kullanılır.
     */
    public void resetSchema() {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                executeSqlFile(conn, "/schema.sql");
                conn.commit();
                System.out.println("Şema yeniden oluşturuldu.");
            } catch (Exception e) {
                conn.rollback();
                throw new DatabaseException("Şema yenilenemedi", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Bağlantı hatası", e);
        }
    }
}
