package com.ogrenci.dao;

import com.ogrenci.exception.DatabaseException;
import com.ogrenci.model.Student;
import com.ogrenci.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) PATTERN:
 *
 * DAO, veritabanı işlemlerini iş mantığından ayırır.
 * Service katmanı DAO'yu çağırır, SQL bilmez.
 * DAO sadece SQL bilir, iş kuralı bilmez.
 *
 *   BankService → StudentDAO → PostgreSQL
 *
 * JDBC TEMEL AKIŞI:
 *   1. Connection al (havuzdan)
 *   2. PreparedStatement oluştur (SQL + parametreler)
 *   3. Parametreleri set et (setString, setInt vb.)
 *   4. executeQuery() veya executeUpdate() çalıştır
 *   5. ResultSet'i oku (SELECT için)
 *   6. Kaynakları kapat (try-with-resources)
 *
 * PREPARED STATEMENT vs STATEMENT:
 *   Statement:          "SELECT * WHERE name = '" + name + "'"  ← SQL INJECTION AÇIĞI!
 *   PreparedStatement:  "SELECT * WHERE name = ?"               ← GÜVENLİ
 *   PreparedStatement her zaman tercih edilmeli!
 */
public class StudentDAO {

    private final DatabaseConnection dbConn;

    public StudentDAO(DatabaseConnection dbConn) {
        this.dbConn = dbConn;
    }

    // ================================================================
    // INSERT — Yeni öğrenci ekle
    // ================================================================

    /**
     * Yeni öğrenci ekler ve üretilen ID'yi döner.
     *
     * PREPARED STATEMENT ile SQL INJECTION önleme:
     *   Kullanıcı "'; DROP TABLE students; --" girse bile
     *   PreparedStatement bunu parametre olarak değil, metin olarak işler.
     *
     * Statement.RETURN_GENERATED_KEYS: INSERT sonrası otomatik üretilen
     * SERIAL (id) değerini almak için gerekli.
     */
    public int insert(Student student) {
        // ? işaretleri: PreparedStatement parametreleri (pozisyona göre 1'den başlar)
        String sql = """
            INSERT INTO students (first_name, last_name, email, birth_date, national_id, grade_level)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        // try-with-resources: conn ve stmt otomatik kapatılır
        try (Connection conn = dbConn.getConnection();
             // RETURN_GENERATED_KEYS: INSERT sonrası üretilen id'yi döner
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Parametreleri pozisyona göre set et (1-indexed)
            stmt.setString(1, student.firstName());   // ? → firstName
            stmt.setString(2, student.lastName());    // ? → lastName
            stmt.setString(3, student.email());       // ? → email

            // null kontrol: LocalDate → java.sql.Date dönüşümü
            if (student.birthDate() != null) {
                stmt.setDate(4, Date.valueOf(student.birthDate())); // LocalDate → SQL Date
            } else {
                stmt.setNull(4, Types.DATE); // NULL değer gönder
            }

            stmt.setString(5, student.nationalId());  // ? → nationalId
            stmt.setInt(6, student.gradeLevel());     // ? → gradeLevel

            // executeUpdate(): INSERT, UPDATE, DELETE için kullanılır
            // Dönen değer: etkilenen satır sayısı
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException("Öğrenci eklenemedi, satır eklenmedi.");
            }

            // Otomatik üretilen ID'yi al
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // getInt(1): ilk kolonu oku
                    return generatedKeys.getInt(1);
                } else {
                    throw new DatabaseException("ID alınamadı.");
                }
            }

        } catch (SQLException e) {
            // PostgreSQL duplicate key hatası kodu: 23505
            if (e.getSQLState().equals("23505")) {
                throw new DatabaseException("Bu e-posta zaten kayıtlı: " + student.email(), e);
            }
            throw new DatabaseException("Öğrenci eklenirken hata: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // SELECT — Öğrenci sorgulama
    // ================================================================

    /**
     * ID ile öğrenci bul.
     * Optional<Student>: öğrenci olmayabilir — null yerine Optional daha güvenli
     */
    public Optional<Student> findById(int id) {
        // TEXT BLOCK (Java 15+): çok satırlı String için
        String sql = """
            SELECT id, first_name, last_name, email, birth_date,
                   national_id, grade_level, is_active, created_at
            FROM students
            WHERE id = ?
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id); // ? → id

            // executeQuery(): SELECT sorgular için ResultSet döner
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) { // Sonuç varsa (satır döndüyse)
                    // ResultSet'i Student Record'a dönüştür
                    return Optional.of(mapResultSetToStudent(rs));
                }
                // Sonuç yoksa boş Optional
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Öğrenci aranırken hata: " + e.getMessage(), e);
        }
    }

    /** Tüm aktif öğrencileri getir. */
    public List<Student> findAll() {
        String sql = """
            SELECT id, first_name, last_name, email, birth_date,
                   national_id, grade_level, is_active, created_at
            FROM students
            WHERE is_active = TRUE
            ORDER BY last_name, first_name
            """;

        // var keyword (Java 10+): tip çıkarımı — List<Student> yazmak zorunda değiliz
        var students = new ArrayList<Student>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) { // Parametresiz sorgu

            // while ile tüm satırları oku
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Öğrenciler listelenirken hata: " + e.getMessage(), e);
        }

        return students;
    }

    /** İsme göre öğrenci ara (ILIKE: büyük/küçük harf duyarsız PostgreSQL operatörü). */
    public List<Student> searchByName(String keyword) {
        String sql = """
            SELECT id, first_name, last_name, email, birth_date,
                   national_id, grade_level, is_active, created_at
            FROM students
            WHERE is_active = TRUE
              AND (first_name ILIKE ? OR last_name ILIKE ? OR email ILIKE ?)
            ORDER BY last_name
            """;

        var students = new ArrayList<Student>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // % joker: CS101 içinde "101" arar — SQL'in contains() gibi
            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Arama hatası: " + e.getMessage(), e);
        }

        return students;
    }

    // ================================================================
    // UPDATE — Öğrenci güncelle
    // ================================================================

    public boolean update(Student student) {
        String sql = """
            UPDATE students
            SET first_name  = ?,
                last_name   = ?,
                email       = ?,
                grade_level = ?,
                updated_at  = NOW()
            WHERE id = ? AND is_active = TRUE
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, student.firstName());
            stmt.setString(2, student.lastName());
            stmt.setString(3, student.email());
            stmt.setInt(4, student.gradeLevel());
            stmt.setInt(5, student.id());

            // executeUpdate(): etkilenen satır sayısını döner
            int updated = stmt.executeUpdate();
            return updated > 0; // Güncelleme oldu mu?

        } catch (SQLException e) {
            throw new DatabaseException("Güncelleme hatası: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // DELETE — Soft delete (pasif yap)
    // ================================================================

    /**
     * Soft delete: fiziksel silme yerine is_active = FALSE yapar.
     * Bu sayede veri tarihçesi korunur.
     */
    public boolean softDelete(int id) {
        String sql = "UPDATE students SET is_active = FALSE, updated_at = NOW() WHERE id = ?";

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Silme hatası: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // BATCH INSERT — Toplu not girişi
    // ================================================================

    /**
     * BATCH PROCESSING: Çok sayıda INSERT'i tek seferde gönderir.
     *
     * Neden batch kullanırız?
     *   100 tekil INSERT: 100 network round-trip → yavaş
     *   1 batch INSERT:   1 network round-trip  → çok hızlı
     *
     * addBatch(): SQL'i kuyruğa ekler (henüz çalışmaz)
     * executeBatch(): kuyruktakilerin hepsini gönderir
     */
    public int[] batchInsert(List<Student> students) {
        String sql = """
            INSERT INTO students (first_name, last_name, email, grade_level)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = dbConn.getConnection()) {
            // Batch için autoCommit kapat
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Her öğrenci için batch'e ekle
                for (Student s : students) {
                    stmt.setString(1, s.firstName());
                    stmt.setString(2, s.lastName());
                    stmt.setString(3, s.email());
                    stmt.setInt(4, s.gradeLevel());
                    stmt.addBatch(); // Bu öğrenciyi kuyruğa ekle
                }

                // Tüm kuyruğu tek seferde veritabanına gönder
                int[] results = stmt.executeBatch();
                conn.commit(); // Transaction'ı onayla
                return results; // Her INSERT için etkilenen satır sayısı

            } catch (SQLException e) {
                conn.rollback(); // Hata olursa geri al
                throw new DatabaseException("Batch insert hatası", e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Bağlantı hatası", e);
        }
    }

    // ================================================================
    // REPORT SORGULARI — JOIN ve Aggregate
    // ================================================================

    /** Öğrenci not ortalamalarını döner (JOIN + GROUP BY). */
    public List<String[]> getStudentAverages() {
        String sql = """
            SELECT
                s.id,
                s.first_name || ' ' || s.last_name AS ad_soyad,
                COUNT(DISTINCT e.course_id)         AS ders_sayisi,
                ROUND(AVG(g.score)::NUMERIC, 2)     AS ortalama,
                MAX(g.score)                        AS en_yuksek,
                MIN(g.score)                        AS en_dusuk
            FROM students s
            LEFT JOIN enrollments e ON s.id = e.student_id AND e.status = 'active'
            LEFT JOIN grades g      ON e.id = g.enrollment_id
            WHERE s.is_active = TRUE
            GROUP BY s.id, s.first_name, s.last_name
            ORDER BY ortalama DESC NULLS LAST
            """;

        var results = new ArrayList<String[]>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                results.add(new String[]{
                    rs.getString("ad_soyad"),
                    String.valueOf(rs.getInt("ders_sayisi")),
                    rs.getString("ortalama") != null ? rs.getString("ortalama") : "—",
                    rs.getString("en_yuksek") != null ? rs.getString("en_yuksek") : "—",
                    rs.getString("en_dusuk") != null ? rs.getString("en_dusuk") : "—"
                });
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ortalama sorgusu hatası: " + e.getMessage(), e);
        }

        return results;
    }

    // ================================================================
    // YARDIMCI — ResultSet → Student Record dönüşümü
    // ================================================================

    /**
     * ResultSet satırını Student Record'a dönüştürür.
     * Builder pattern ile okunabilir nesne oluşturma.
     */
    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        // rs.getXxx("kolon_adı"): kolonu ismiyle oku (indeksle de okunabilir ama isim daha güvenli)
        var builder = Student.builder()
            .id(rs.getInt("id"))
            .firstName(rs.getString("first_name"))
            .lastName(rs.getString("last_name"))
            .email(rs.getString("email"))
            .nationalId(rs.getString("national_id"))
            .gradeLevel(rs.getInt("grade_level"))
            .isActive(rs.getBoolean("is_active"));

        // null olabilecek alanlar için kontrol
        Date birthDate = rs.getDate("birth_date");
        if (birthDate != null) {
            builder.birthDate(birthDate.toLocalDate()); // SQL Date → LocalDate
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            builder.createdAt(createdAt.toLocalDateTime()); // SQL Timestamp → LocalDateTime
        }

        return builder.build();
    }
}
