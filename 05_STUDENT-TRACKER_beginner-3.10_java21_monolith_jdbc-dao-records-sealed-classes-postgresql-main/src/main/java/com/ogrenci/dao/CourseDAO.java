package com.ogrenci.dao;

import com.ogrenci.exception.DatabaseException;
import com.ogrenci.model.Course;
import com.ogrenci.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Ders veritabanı işlemleri — INNER JOIN ve LEFT JOIN örnekleri. */
public class CourseDAO {

    private final DatabaseConnection dbConn;

    public CourseDAO(DatabaseConnection dbConn) {
        this.dbConn = dbConn;
    }

    public int insert(Course course) {
        String sql = """
            INSERT INTO courses (course_code, course_name, teacher_id, credits, description, capacity)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, course.courseCode());
            stmt.setString(2, course.courseName());

            // teacherId 0 ise NULL kaydet (öğretmensiz ders)
            if (course.teacherId() > 0) {
                stmt.setInt(3, course.teacherId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setInt(4, course.credits());
            stmt.setString(5, course.description());
            stmt.setInt(6, course.capacity());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Ders ID alınamadı.");
            }

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                throw new DatabaseException("Bu ders kodu zaten mevcut: " + course.courseCode(), e);
            }
            throw new DatabaseException("Ders eklenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * LEFT JOIN ile ders + öğretmen bilgisi.
     * Öğretmensiz ders olsa bile satır döner (LEFT JOIN özelliği).
     */
    public List<Course> findAll() {
        String sql = """
            SELECT c.id, c.course_code, c.course_name, c.teacher_id,
                   COALESCE(t.first_name || ' ' || t.last_name, 'Atanmadı') AS teacher_name,
                   c.credits, c.description, c.capacity, c.is_active, c.created_at
            FROM courses c
            LEFT JOIN teachers t ON c.teacher_id = t.id
            WHERE c.is_active = TRUE
            ORDER BY c.course_code
            """;

        // COALESCE(a, b): a NULL ise b döner — öğretmensiz dersler için fallback

        var list = new ArrayList<Course>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new DatabaseException("Dersler listelenemedi", e);
        }

        return list;
    }

    public Optional<Course> findById(int id) {
        String sql = """
            SELECT c.*, COALESCE(t.first_name || ' ' || t.last_name, 'Atanmadı') AS teacher_name
            FROM courses c LEFT JOIN teachers t ON c.teacher_id = t.id
            WHERE c.id = ?
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ders aranırken hata", e);
        }
    }

    public Optional<Course> findByCode(String code) {
        String sql = """
            SELECT c.*, COALESCE(t.first_name || ' ' || t.last_name, 'Atanmadı') AS teacher_name
            FROM courses c LEFT JOIN teachers t ON c.teacher_id = t.id
            WHERE c.course_code = ?
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code.toUpperCase());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ders sorgusu hatası", e);
        }
    }

    /**
     * Öğretmene göre dersler — WHERE ile foreign key filtresi.
     */
    public List<Course> findByTeacher(int teacherId) {
        String sql = """
            SELECT c.*, t.first_name || ' ' || t.last_name AS teacher_name
            FROM courses c INNER JOIN teachers t ON c.teacher_id = t.id
            WHERE c.teacher_id = ? AND c.is_active = TRUE
            ORDER BY c.course_code
            """;

        var list = new ArrayList<Course>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, teacherId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Öğretmen dersleri listelenemedi", e);
        }

        return list;
    }

    public boolean update(Course course) {
        String sql = """
            UPDATE courses SET course_name=?, teacher_id=?, credits=?, capacity=?
            WHERE id=? AND is_active=TRUE
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, course.courseName());
            if (course.teacherId() > 0) {
                stmt.setInt(2, course.teacherId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, course.credits());
            stmt.setInt(4, course.capacity());
            stmt.setInt(5, course.id());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Ders güncellenemedi", e);
        }
    }

    public boolean softDelete(int id) {
        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE courses SET is_active = FALSE WHERE id = ?")) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Ders silinemedi", e);
        }
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        return Course.builder()
            .id(rs.getInt("id"))
            .courseCode(rs.getString("course_code"))
            .courseName(rs.getString("course_name"))
            .teacherId(rs.getInt("teacher_id"))
            .teacherName(rs.getString("teacher_name"))
            .credits(rs.getInt("credits"))
            .description(rs.getString("description") != null ? rs.getString("description") : "")
            .capacity(rs.getInt("capacity"))
            .isActive(rs.getBoolean("is_active"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();
    }
}
