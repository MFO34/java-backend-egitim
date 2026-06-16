package com.ogrenci.dao;

import com.ogrenci.exception.DatabaseException;
import com.ogrenci.model.Grade;
import com.ogrenci.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Not DAO — BATCH INSERT, HAVING, Aggregate fonksiyonlar.
 */
public class GradeDAO {

    private final DatabaseConnection dbConn;

    public GradeDAO(DatabaseConnection dbConn) {
        this.dbConn = dbConn;
    }

    /** Tek not ekle. */
    public int insert(Grade grade) {
        String sql = """
            INSERT INTO grades (enrollment_id, grade_type, score, comment)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, grade.enrollmentId());
            stmt.setString(2, grade.gradeType());
            stmt.setBigDecimal(3, grade.score());

            // Null olabilecek alan: setNull ile
            if (grade.comment() != null) {
                stmt.setString(4, grade.comment());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Not ID alınamadı.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Not eklenemedi: " + e.getMessage(), e);
        }
    }

    /** BATCH INSERT — Toplu not girişi. */
    public void batchInsert(List<Grade> grades) {
        String sql = "INSERT INTO grades (enrollment_id, grade_type, score) VALUES (?, ?, ?)";

        try (Connection conn = dbConn.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Grade g : grades) {
                    stmt.setInt(1, g.enrollmentId());
                    stmt.setString(2, g.gradeType());
                    stmt.setBigDecimal(3, g.score());
                    stmt.addBatch(); // Kuyruğa ekle
                }

                stmt.executeBatch(); // Hepsini gönder
                conn.commit();
                System.out.println(grades.size() + " not toplu olarak girildi.");

            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Toplu not girişi başarısız", e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Bağlantı hatası", e);
        }
    }

    /** Enrollment'a ait notları getir. */
    public List<Grade> findByEnrollment(int enrollmentId) {
        String sql = """
            SELECT g.id, g.enrollment_id, g.grade_type, g.score, g.graded_at, g.comment,
                   s.first_name || ' ' || s.last_name AS student_name,
                   c.course_code
            FROM grades g
            INNER JOIN enrollments e ON g.enrollment_id = e.id
            INNER JOIN students s    ON e.student_id = s.id
            INNER JOIN courses c     ON e.course_id  = c.id
            WHERE g.enrollment_id = ?
            ORDER BY g.graded_at
            """;

        var list = new ArrayList<Grade>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, enrollmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Notlar listelenemedi", e);
        }

        return list;
    }

    /**
     * En yüksek ortalamalı N öğrenciyi getir.
     * ORDER BY + LIMIT + HAVING birlikte.
     */
    public List<String[]> getTopStudents(int limit) {
        String sql = """
            SELECT
                s.first_name || ' ' || s.last_name AS ad_soyad,
                ROUND(AVG(g.score)::NUMERIC, 2)    AS ortalama,
                COUNT(g.id)                        AS not_sayisi
            FROM students s
            INNER JOIN enrollments e ON s.id = e.student_id
            INNER JOIN grades g      ON e.id = g.enrollment_id
            WHERE s.is_active = TRUE
            GROUP BY s.id, s.first_name, s.last_name
            HAVING COUNT(g.id) > 0       -- En az 1 notu olanlar
            ORDER BY ortalama DESC
            LIMIT ?                      -- İlk N kayıt
            """;

        var results = new ArrayList<String[]>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[]{
                        rs.getString("ad_soyad"),
                        rs.getString("ortalama"),
                        String.valueOf(rs.getInt("not_sayisi"))
                    });
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Top öğrenciler sorgusu hatası", e);
        }

        return results;
    }

    /** Not güncelle. */
    public boolean update(int gradeId, BigDecimal newScore, String comment) {
        String sql = "UPDATE grades SET score=?, comment=?, graded_at=NOW() WHERE id=?";

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, newScore);
            if (comment != null) stmt.setString(2, comment);
            else stmt.setNull(2, Types.VARCHAR);
            stmt.setInt(3, gradeId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Not güncellenemedi", e);
        }
    }

    /** Ders bazında not ortalaması raporu (GROUP BY + AVG). */
    public List<String[]> getCourseAverages() {
        String sql = """
            SELECT
                c.course_code,
                c.course_name,
                COUNT(DISTINCT e.student_id)     AS ogrenci_sayisi,
                ROUND(AVG(g.score)::NUMERIC, 2)  AS ortalama,
                MAX(g.score)                     AS en_yuksek,
                MIN(g.score)                     AS en_dusuk,
                SUM(g.score)                     AS toplam_puan
            FROM courses c
            LEFT JOIN enrollments e ON c.id = e.course_id AND e.status = 'active'
            LEFT JOIN grades g      ON e.id = g.enrollment_id
            WHERE c.is_active = TRUE
            GROUP BY c.id, c.course_code, c.course_name
            ORDER BY ortalama DESC NULLS LAST
            """;

        var results = new ArrayList<String[]>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                results.add(new String[]{
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    String.valueOf(rs.getInt("ogrenci_sayisi")),
                    rs.getString("ortalama") != null ? rs.getString("ortalama") : "—",
                    rs.getString("en_yuksek") != null ? rs.getString("en_yuksek") : "—",
                    rs.getString("en_dusuk") != null ? rs.getString("en_dusuk") : "—"
                });
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ders ortalamaları sorgusu hatası", e);
        }

        return results;
    }

    private Grade mapRow(ResultSet rs) throws SQLException {
        return Grade.builder()
            .id(rs.getInt("id"))
            .enrollmentId(rs.getInt("enrollment_id"))
            .gradeType(rs.getString("grade_type"))
            .score(rs.getBigDecimal("score"))
            .gradedAt(rs.getTimestamp("graded_at") != null
                    ? rs.getTimestamp("graded_at").toLocalDateTime() : null)
            .comment(rs.getString("comment"))
            .studentName(rs.getString("student_name") != null
                    ? rs.getString("student_name") : "")
            .courseCode(rs.getString("course_code") != null
                    ? rs.getString("course_code") : "")
            .build();
    }
}
