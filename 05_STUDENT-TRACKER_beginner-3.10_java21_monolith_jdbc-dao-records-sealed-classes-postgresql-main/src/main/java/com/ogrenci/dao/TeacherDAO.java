package com.ogrenci.dao;

import com.ogrenci.exception.DatabaseException;
import com.ogrenci.model.Teacher;
import com.ogrenci.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Öğretmen veritabanı işlemleri. */
public class TeacherDAO {

    private final DatabaseConnection dbConn;

    public TeacherDAO(DatabaseConnection dbConn) {
        this.dbConn = dbConn;
    }

    public int insert(Teacher teacher) {
        String sql = """
            INSERT INTO teachers (first_name, last_name, email, department, salary)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, teacher.firstName());
            stmt.setString(2, teacher.lastName());
            stmt.setString(3, teacher.email());
            stmt.setString(4, teacher.department());
            // setBigDecimal: DECIMAL(10,2) için doğru tip
            stmt.setBigDecimal(5, teacher.salary());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("Öğretmen ID alınamadı.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Öğretmen eklenirken hata: " + e.getMessage(), e);
        }
    }

    public Optional<Teacher> findById(int id) {
        String sql = "SELECT * FROM teachers WHERE id = ?";

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Öğretmen aranırken hata", e);
        }
    }

    public List<Teacher> findAll() {
        String sql = "SELECT * FROM teachers WHERE is_active = TRUE ORDER BY last_name";
        var list = new ArrayList<Teacher>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            throw new DatabaseException("Öğretmenler listelenemedi", e);
        }

        return list;
    }

    /** Departmana göre öğretmenler — WHERE filtresi örneği. */
    public List<Teacher> findByDepartment(String department) {
        String sql = "SELECT * FROM teachers WHERE department ILIKE ? AND is_active = TRUE";
        var list = new ArrayList<Teacher>();

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + department + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Departman sorgusu hatası", e);
        }

        return list;
    }

    public boolean update(Teacher teacher) {
        String sql = """
            UPDATE teachers SET first_name=?, last_name=?, department=?, salary=?
            WHERE id=? AND is_active=TRUE
            """;

        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teacher.firstName());
            stmt.setString(2, teacher.lastName());
            stmt.setString(3, teacher.department());
            stmt.setBigDecimal(4, teacher.salary());
            stmt.setInt(5, teacher.id());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Güncelleme hatası", e);
        }
    }

    public boolean softDelete(int id) {
        try (Connection conn = dbConn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE teachers SET is_active = FALSE WHERE id = ?")) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Silme hatası", e);
        }
    }

    private Teacher mapRow(ResultSet rs) throws SQLException {
        return Teacher.builder()
            .id(rs.getInt("id"))
            .firstName(rs.getString("first_name"))
            .lastName(rs.getString("last_name"))
            .email(rs.getString("email"))
            .department(rs.getString("department"))
            .salary(rs.getBigDecimal("salary") != null
                    ? rs.getBigDecimal("salary") : BigDecimal.ZERO)
            .isActive(rs.getBoolean("is_active"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();
    }
}
