package com.ogrenci.app;

import com.ogrenci.dao.*;
import com.ogrenci.service.*;
import com.ogrenci.util.DatabaseConnection;
import com.ogrenci.util.DatabaseInitializer;

/**
 * Uygulamanın giriş noktası.
 *
 * Bağımlılık Ağacı:
 *   DatabaseConnection (Singleton)
 *       ↓
 *   DAO'lar (StudentDAO, CourseDAO, ...)
 *       ↓
 *   Service'ler (StudentService, CourseService, ...)
 *       ↓
 *   ConsoleMenu
 */
public class OgrenciApp {

    public static void main(String[] args) {
        System.out.println("Öğrenci Takip Sistemi başlatılıyor...");
        System.out.println("Not: PostgreSQL'in çalıştığından emin olun.");
        System.out.println("     docker-compose up -d db\n");

        // Singleton: tüm uygulama boyunca tek bir bağlantı havuzu
        DatabaseConnection dbConn = DatabaseConnection.getInstance();

        // Veritabanı tablolarını oluştur ve örnek verileri yükle
        DatabaseInitializer initializer = new DatabaseInitializer(dbConn);
        try {
            initializer.initialize();
        } catch (Exception e) {
            System.err.println("Veritabanı başlatma hatası: " + e.getMessage());
            System.err.println("PostgreSQL çalışıyor mu? docker-compose up -d db");
            System.exit(1); // Hata kodu 1 ile çık
        }

        // DAO'ları oluştur
        StudentDAO    studentDAO    = new StudentDAO(dbConn);
        TeacherDAO    teacherDAO    = new TeacherDAO(dbConn);
        CourseDAO     courseDAO     = new CourseDAO(dbConn);
        EnrollmentDAO enrollmentDAO = new EnrollmentDAO(dbConn);
        GradeDAO      gradeDAO      = new GradeDAO(dbConn);

        // Servisleri oluştur (DAO'ları inject et — Dependency Injection)
        StudentService studentService = new StudentService(studentDAO);
        CourseService  courseService  = new CourseService(courseDAO, teacherDAO, enrollmentDAO);
        GradeService   gradeService   = new GradeService(gradeDAO, enrollmentDAO);

        // Menüyü başlat
        ConsoleMenu menu = new ConsoleMenu(studentService, courseService, gradeService);

        // JVM kapanırken bağlantı havuzunu kapat (shutdown hook)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nUygulama kapatılıyor...");
            dbConn.shutdown();
        }));

        // Ana menü döngüsü
        menu.start();
    }
}
