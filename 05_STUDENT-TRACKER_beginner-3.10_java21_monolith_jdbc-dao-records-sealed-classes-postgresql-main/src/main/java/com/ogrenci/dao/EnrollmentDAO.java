package com.ogrenci.dao;

import com.ogrenci.exception.DatabaseException;
import com.ogrenci.model.Enrollment;
import com.ogrenci.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Öğrenci-ders kaydı (enrollment) DAO.
 * Üçlü JOIN örneği: students + enrollments + courses.
 */
public class EnrollmentDAO {

    private final DatabaseConnection dbConn;

    public EnrollmentDAO(DatabaseConnection dbConn) {
        this.dbConn = dbConn;
    }

    /** Öğrenciyi derse kaydet. */
    public int enroll(int studentId, int courseId) {
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Kayıt ID alınamadı.");
            }

        } catch (SQLException e) {
            // UNIQUE constraint ihlali: 23505
            if (e.getSQLState().equals("23505")) {
                throw new DatabaseException("Bu öğrenci zaten bu derse kayıtlı.", e);
            }
            throw new DatabaseException("Ders kaydı oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /** Kaydı iptal et (dropped). */
    public boolean drop(int studentId, int courseId) {
        String sql = "UPDATE enrollments SET status='dropped' WHERE student_id=? AND course_id=?";

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Kayıt silinemedi", e);
        }
    }

    /**
     * Derse kayıtlı öğrencileri getir.
     * INNER JOIN: students + enrollments + courses
     */
    public List<Enrollment> findByCourse(int courseId) {
        String sql = """
            SELECT e.id, e.student_id, e.course_id,
                   s.first_name || ' ' || s.last_name AS student_name,
                   c.course_name, c.course_code,
                   e.enrolled_at, e.status
            FROM enrollments e
            INNER JOIN students s ON e.student_id = s.id
            INNER JOIN courses c  ON e.course_id  = c.id
            WHERE e.course_id = ? AND e.status = 'active'
            ORDER BY s.last_name
            """;

        return queryList(sql, stmt -> stmt.setInt(1, courseId));
    }

    /** Öğrencinin kayıtlı olduğu dersleri getir. */
    public List<Enrollment> findByStudent(int studentId) {
        String sql = """
            SELECT e.id, e.student_id, e.course_id,
                   s.first_name || ' ' || s.last_name AS student_name,
                   c.course_name, c.course_code,
                   e.enrolled_at, e.status
            FROM enrollments e
            INNER JOIN students s ON e.student_id = s.id
            INNER JOIN courses c  ON e.course_id  = c.id
            WHERE e.student_id = ? AND e.status = 'active'
            ORDER BY c.course_code
            """;

        return queryList(sql, stmt -> stmt.setInt(1, studentId));
    }

    /** Enrollment ID ile bul (not girmek için gerekli). */
    public Optional<Enrollment> findById(int id) {
        String sql = """
            SELECT e.id, e.student_id, e.course_id,
                   s.first_name || ' ' || s.last_name AS student_name,
                   c.course_name, c.course_code, e.enrolled_at, e.status
            FROM enrollments e
            INNER JOIN students s ON e.student_id = s.id
            INNER JOIN courses c  ON e.course_id  = c.id
            WHERE e.id = ?
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Kayıt aranırken hata", e);
        }
    }

    // Yardımcı: SQL + parametre setter → Liste döner (lambda ile)
    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement stmt) throws SQLException;
    }

    private List<Enrollment> queryList(String sql, StatementSetter setter) {
        var list = new ArrayList<Enrollment>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setter.set(stmt); // Lambda ile parametreyi set et

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Kayıtlar listelenemedi", e);
        }

        return list;
    }

    private Enrollment mapRow(ResultSet rs) throws SQLException {
        return Enrollment.builder()
            .id(rs.getInt("id"))
            .studentId(rs.getInt("student_id"))
            .courseId(rs.getInt("course_id"))
            .studentName(rs.getString("student_name"))
            .courseName(rs.getString("course_name"))
            .courseCode(rs.getString("course_code"))
            .enrolledAt(rs.getTimestamp("enrolled_at").toLocalDateTime())
            .status(rs.getString("status"))
            .build();
    }
}
