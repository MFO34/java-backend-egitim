package com.ogrenci.service;

import com.ogrenci.dao.EnrollmentDAO;
import com.ogrenci.dao.GradeDAO;
import com.ogrenci.model.Grade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Not iş mantığı katmanı. */
public class GradeService {

    private final GradeDAO gradeDAO;
    private final EnrollmentDAO enrollmentDAO;

    public GradeService(GradeDAO gradeDAO, EnrollmentDAO enrollmentDAO) {
        this.gradeDAO = gradeDAO;
        this.enrollmentDAO = enrollmentDAO;
    }

    /** Not ekle. */
    public int addGrade(Grade grade) {
        // Enrollment varlık kontrolü
        enrollmentDAO.findById(grade.enrollmentId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Kayıt bulunamadı: enrollment_id=" + grade.enrollmentId()));

        return gradeDAO.insert(grade);
    }

    /** Not güncelle. */
    public boolean updateGrade(int gradeId, double newScore, String comment) {
        return gradeDAO.update(gradeId, BigDecimal.valueOf(newScore), comment);
    }

    /** Enrollment'a ait tüm notları getir ve yazdır. */
    public void printGradesForEnrollment(int enrollmentId) {
        var grades = gradeDAO.findByEnrollment(enrollmentId);

        if (grades.isEmpty()) {
            System.out.println("Bu kayıt için henüz not girilmemiş.");
            return;
        }

        System.out.printf("%-12s | %-8s | %-6s%n", "Tür", "Puan", "Harf");
        System.out.println("-".repeat(35));

        grades.forEach(g ->
            System.out.printf("%-12s | %-8.2f | %s%n",
                g.gradeType(), g.score().doubleValue(), g.letterGrade())
        );
    }

    /**
     * BATCH INSERT — Birden fazla nota toplu giriş.
     * Not listesi hazırlanıp tek seferde kaydedilir.
     */
    public void batchAddGrades(List<Grade> grades) {
        gradeDAO.batchInsert(grades);
    }

    /** En yüksek notlu öğrencileri yazdır. */
    public void printTopStudents(int limit) {
        var data = gradeDAO.getTopStudents(limit);

        System.out.println("\n=== EN BAŞARILI " + limit + " ÖĞRENCİ ===");
        System.out.printf("%-3s %-25s %-10s %-8s%n", "Sıra", "Öğrenci", "Ortalama", "Not Sayısı");
        System.out.println("-".repeat(50));

        for (int i = 0; i < data.size(); i++) {
            var row = data.get(i);
            System.out.printf("%-3d %-25s %-10s %-8s%n",
                i + 1, row[0], row[1], row[2]);
        }
    }

    /** Ders bazında not ortalamaları raporu. */
    public void printCourseAverages() {
        var data = gradeDAO.getCourseAverages();

        System.out.println("\n=== DERS NOT ORTALAMALARINIZ ===");
        System.out.printf("%-8s %-30s %-8s %-8s %-8s %-8s%n",
            "Kod", "Ders Adı", "Öğrenci", "Ort.", "Max", "Min");
        System.out.println("-".repeat(75));

        data.forEach(row ->
            System.out.printf("%-8s %-30s %-8s %-8s %-8s %-8s%n",
                row[0], row[1], row[2], row[3], row[4], row[5])
        );
    }

    /** Örnek toplu not listesi oluştur (demo için). */
    public List<Grade> createSampleBatchGrades(List<Integer> enrollmentIds, String gradeType) {
        var grades = new ArrayList<Grade>();

        for (int enrollmentId : enrollmentIds) {
            // Her kayıt için rastgele bir not oluştur (demo)
            double score = 50 + Math.random() * 50; // 50-100 arası
            grades.add(Grade.builder()
                .enrollmentId(enrollmentId)
                .gradeType(gradeType)
                .score(BigDecimal.valueOf(Math.round(score * 10.0) / 10.0))
                .build());
        }

        return grades;
    }
}
