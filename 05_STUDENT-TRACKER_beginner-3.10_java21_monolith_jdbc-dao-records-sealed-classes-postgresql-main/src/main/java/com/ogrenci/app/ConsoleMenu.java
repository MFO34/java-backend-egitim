package com.ogrenci.app;

import com.ogrenci.exception.CourseNotFoundException;
import com.ogrenci.exception.StudentNotFoundException;
import com.ogrenci.model.*;
import com.ogrenci.service.CourseService;
import com.ogrenci.service.GradeService;
import com.ogrenci.service.StudentService;
import com.ogrenci.util.ConsoleHelper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Konsol menü yöneticisi.
 *
 * JAVA 21 PATTERN MATCHING FOR SWITCH:
 * QueryResult (sealed interface) switch ile işlenir.
 * Her alt tip (Success, Empty, Failure) ayrı case bloğunda ele alınır.
 */
public class ConsoleMenu {

    private final StudentService studentService;
    private final CourseService  courseService;
    private final GradeService   gradeService;

    public ConsoleMenu(StudentService s, CourseService c, GradeService g) {
        this.studentService = s;
        this.courseService  = c;
        this.gradeService   = g;
    }

    /** Ana menü döngüsü. */
    public void start() {
        boolean running = true;

        while (running) {
            ConsoleHelper.printHeader("ÖĞRENCİ TAKİP SİSTEMİ");
            System.out.println("  1. Öğrenci İşlemleri");
            System.out.println("  2. Öğretmen İşlemleri");
            System.out.println("  3. Ders İşlemleri");
            System.out.println("  4. Ders Kayıt / İptal");
            System.out.println("  5. Not İşlemleri");
            System.out.println("  6. Raporlar");
            System.out.println("  0. Çıkış");
            System.out.println("=".repeat(60));

            int choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> studentMenu();
                case 2 -> teacherMenu();
                case 3 -> courseMenu();
                case 4 -> enrollmentMenu();
                case 5 -> gradeMenu();
                case 6 -> reportMenu();
                case 0 -> running = false;
                default -> System.out.println("Geçersiz seçim.");
            }
        }
    }

    // ================================================================
    // ÖĞRENCİ MENÜSÜ
    // ================================================================
    private void studentMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("ÖĞRENCİ İŞLEMLERİ");
            System.out.println("  1. Yeni Öğrenci Ekle");
            System.out.println("  2. Tüm Öğrencileri Listele");
            System.out.println("  3. Öğrenci Ara");
            System.out.println("  4. Öğrenci Detayı");
            System.out.println("  5. Sınıfa Göre Grupla");
            System.out.println("  6. Öğrenci Güncelle");
            System.out.println("  7. Öğrenci Sil (Pasifleştir)");
            System.out.println("  0. Geri");
            System.out.println("-".repeat(60));
            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> addStudent();
                case 2 -> listStudents();
                case 3 -> searchStudent();
                case 4 -> showStudentDetail();
                case 5 -> studentService.printStudentsByGrade();
                case 6 -> updateStudent();
                case 7 -> deleteStudent();
            }
            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    private void addStudent() {
        System.out.println("\n--- YENİ ÖĞRENCİ ---");
        var firstName  = ConsoleHelper.readString("Ad         : ");
        var lastName   = ConsoleHelper.readString("Soyad      : ");
        var email      = ConsoleHelper.readString("E-posta    : ");
        var nationalId = ConsoleHelper.readString("TC Kimlik  : ");
        var gradeLevel = ConsoleHelper.readInt("Sınıf (1-4): ");

        try {
            // Builder pattern ile Student oluştur
            var student = Student.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .nationalId(nationalId.isBlank() ? null : nationalId)
                .gradeLevel(gradeLevel)
                .build();

            // SEALED CLASS PATTERN MATCHING (Java 21)
            // QueryResult'ın hangi alt tipi olduğuna göre farklı işlem yap
            QueryResult<Student> result = studentService.addStudent(student);

            // switch expression ile sealed interface pattern matching
            switch (result) {
                case QueryResult.Success<Student> s ->
                    System.out.println("Öğrenci eklendi! ID: " + s.data().id()
                                       + " | " + s.data().fullName());
                case QueryResult.Failure<Student> f ->
                    System.out.println("Hata: " + f.message());
                default ->
                    System.out.println("Beklenmedik durum.");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Doğrulama hatası: " + e.getMessage());
        }
    }

    private void listStudents() {
        var students = studentService.getAllStudents();
        if (students.isEmpty()) { System.out.println("Kayıtlı öğrenci yok."); return; }

        System.out.printf("\n%-6s %-25s %-30s %-5s%n", "ID", "Ad Soyad", "E-posta", "Sınıf");
        System.out.println("-".repeat(70));
        students.forEach(s ->
            System.out.printf("%-6d %-25s %-30s %-5d%n",
                s.id(), s.fullName(), s.email(), s.gradeLevel())
        );
        System.out.println("Toplam: " + students.size() + " öğrenci");
    }

    private void searchStudent() {
        var keyword = ConsoleHelper.readString("Arama: ");
        var results = studentService.searchStudents(keyword);
        if (results.isEmpty()) { System.out.println("Sonuç bulunamadı."); return; }

        results.forEach(s ->
            System.out.printf("[%d] %s - %s%n", s.id(), s.fullName(), s.email())
        );
    }

    private void showStudentDetail() {
        int id = ConsoleHelper.readInt("Öğrenci ID: ");
        try {
            var s = studentService.getStudentById(id);
            System.out.println("\n--- " + s.fullName() + " ---");
            System.out.println("ID       : " + s.id());
            System.out.println("E-posta  : " + s.email());
            System.out.println("TC Kimlik: " + (s.nationalId() != null ? s.nationalId() : "—"));
            System.out.println("Sınıf    : " + s.gradeLevel());
            System.out.println("Durum    : " + (s.isActive() ? "Aktif" : "Pasif"));

            // Kayıtlı dersler
            var enrollments = courseService.getStudentEnrollments(id);
            System.out.println("\nKayıtlı Dersler:");
            if (enrollments.isEmpty()) {
                System.out.println("  Henüz ders kaydı yok.");
            } else {
                enrollments.forEach(e ->
                    System.out.printf("  [%d] %s — %s (%s)%n",
                        e.id(), e.courseCode(), e.courseName(), e.status())
                );
            }
        } catch (StudentNotFoundException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private void updateStudent() {
        int id = ConsoleHelper.readInt("Güncellenecek öğrenci ID: ");
        try {
            var existing = studentService.getStudentById(id);
            System.out.println("Mevcut: " + existing.fullName() + " | Sınıf: " + existing.gradeLevel());

            var firstName  = ConsoleHelper.readString("Yeni Ad (boş=değiştirme)     : ");
            var lastName   = ConsoleHelper.readString("Yeni Soyad (boş=değiştirme) : ");
            var gradeLevel = ConsoleHelper.readInt("Yeni Sınıf (0=değiştirme)    : ");

            // var: tip çıkarımı (Java 10+)
            var updated = Student.builder()
                .id(id)
                .firstName(firstName.isBlank() ? existing.firstName() : firstName)
                .lastName(lastName.isBlank() ? existing.lastName() : lastName)
                .email(existing.email())
                .nationalId(existing.nationalId())
                .gradeLevel(gradeLevel == 0 ? existing.gradeLevel() : gradeLevel)
                .isActive(existing.isActive())
                .build();

            boolean ok = studentService.updateStudent(updated);
            System.out.println(ok ? "Güncellendi." : "Güncellenemedi.");
        } catch (StudentNotFoundException e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private void deleteStudent() {
        int id = ConsoleHelper.readInt("Silinecek öğrenci ID: ");
        if (ConsoleHelper.readYesNo("Emin misiniz?")) {
            try {
                boolean ok = studentService.deleteStudent(id);
                System.out.println(ok ? "Öğrenci pasifleştirildi." : "İşlem başarısız.");
            } catch (StudentNotFoundException e) {
                System.out.println("Hata: " + e.getMessage());
            }
        }
    }

    // ================================================================
    // ÖĞRETMEN MENÜSÜ
    // ================================================================
    private void teacherMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("ÖĞRETMEN İŞLEMLERİ");
            System.out.println("  1. Öğretmen Ekle");
            System.out.println("  2. Öğretmenleri Listele");
            System.out.println("  3. Öğretmen-Ders Eşleştirmesi");
            System.out.println("  0. Geri");
            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> addTeacher();
                case 2 -> listTeachers();
                case 3 -> courseService.printTeacherCourses();
            }
            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    private void addTeacher() {
        var firstName  = ConsoleHelper.readString("Ad        : ");
        var lastName   = ConsoleHelper.readString("Soyad     : ");
        var email      = ConsoleHelper.readString("E-posta   : ");
        var department = ConsoleHelper.readString("Bölüm     : ");
        var salary     = ConsoleHelper.readDouble("Maaş (TL) : ");

        var teacher = Teacher.builder()
            .firstName(firstName).lastName(lastName)
            .email(email).department(department)
            .salary(BigDecimal.valueOf(salary))
            .build();

        int id = courseService.addTeacher(teacher);
        System.out.println("Öğretmen eklendi. ID: " + id);
    }

    private void listTeachers() {
        var teachers = courseService.getAllTeachers();
        System.out.printf("\n%-6s %-25s %-30s %-20s%n", "ID", "Ad Soyad", "E-posta", "Bölüm");
        System.out.println("-".repeat(85));
        teachers.forEach(t ->
            System.out.printf("%-6d %-25s %-30s %-20s%n",
                t.id(), t.fullName(), t.email(), t.department())
        );
    }

    // ================================================================
    // DERS MENÜSÜ
    // ================================================================
    private void courseMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("DERS İŞLEMLERİ");
            System.out.println("  1. Ders Ekle");
            System.out.println("  2. Dersleri Listele");
            System.out.println("  0. Geri");
            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> addCourse();
                case 2 -> listCourses();
            }
            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    private void addCourse() {
        var code       = ConsoleHelper.readString("Ders Kodu   : ");
        var name       = ConsoleHelper.readString("Ders Adı    : ");
        int teacherId  = ConsoleHelper.readInt("Öğretmen ID (0=yok): ");
        int credits    = ConsoleHelper.readInt("Kredi (1-6) : ");
        int capacity   = ConsoleHelper.readInt("Kontenjan   : ");

        var course = Course.builder()
            .courseCode(code.toUpperCase())
            .courseName(name)
            .teacherId(teacherId)
            .credits(credits)
            .capacity(capacity)
            .build();

        try {
            int id = courseService.addCourse(course);
            System.out.println("Ders eklendi. ID: " + id);
        } catch (Exception e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private void listCourses() {
        var courses = courseService.getAllCourses();
        System.out.printf("\n%-6s %-8s %-30s %-25s %-6s %-8s%n",
            "ID", "Kod", "Ders Adı", "Öğretmen", "Kredi", "Kontenjan");
        System.out.println("-".repeat(85));
        courses.forEach(c ->
            System.out.printf("%-6d %-8s %-30s %-25s %-6d %-8d%n",
                c.id(), c.courseCode(), c.courseName(),
                c.teacherName(), c.credits(), c.capacity())
        );
    }

    // ================================================================
    // KAYIT MENÜSÜ
    // ================================================================
    private void enrollmentMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("DERS KAYIT / İPTAL");
            System.out.println("  1. Öğrenciyi Derse Kaydet");
            System.out.println("  2. Ders Kaydını İptal Et");
            System.out.println("  3. Dersteki Öğrencileri Listele");
            System.out.println("  0. Geri");
            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> {
                    int sId = ConsoleHelper.readInt("Öğrenci ID : ");
                    int cId = ConsoleHelper.readInt("Ders ID    : ");
                    try {
                        int eId = courseService.enrollStudent(sId, cId);
                        System.out.println("Kayıt oluşturuldu. Enrollment ID: " + eId);
                    } catch (Exception e) {
                        System.out.println("Hata: " + e.getMessage());
                    }
                }
                case 2 -> {
                    int sId = ConsoleHelper.readInt("Öğrenci ID : ");
                    int cId = ConsoleHelper.readInt("Ders ID    : ");
                    boolean ok = courseService.dropStudent(sId, cId);
                    System.out.println(ok ? "Kayıt iptal edildi." : "Kayıt bulunamadı.");
                }
                case 3 -> {
                    int cId = ConsoleHelper.readInt("Ders ID    : ");
                    var list = courseService.getStudentsInCourse(cId);
                    if (list.isEmpty()) { System.out.println("Bu derse kayıtlı öğrenci yok."); break; }
                    list.forEach(e ->
                        System.out.printf("[%d] %s (%s)%n", e.studentId(), e.studentName(), e.status())
                    );
                }
            }
            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    // ================================================================
    // NOT MENÜSÜ
    // ================================================================
    private void gradeMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("NOT İŞLEMLERİ");
            System.out.println("  1. Not Gir");
            System.out.println("  2. Not Güncelle");
            System.out.println("  3. Notları Görüntüle (enrollment ID ile)");
            System.out.println("  4. Toplu Not Girişi (Batch)");
            System.out.println("  0. Geri");
            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> addGrade();
                case 2 -> updateGrade();
                case 3 -> {
                    int eId = ConsoleHelper.readInt("Enrollment ID: ");
                    gradeService.printGradesForEnrollment(eId);
                }
                case 4 -> batchGrades();
            }
            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }

    private void addGrade() {
        int eId    = ConsoleHelper.readInt("Enrollment ID  : ");
        var type   = ConsoleHelper.readString("Not Türü (midterm/final/quiz): ");
        double score = ConsoleHelper.readDouble("Puan (0-100)   : ");
        var comment = ConsoleHelper.readString("Yorum (boş geçilebilir): ");

        var grade = Grade.builder()
            .enrollmentId(eId)
            .gradeType(type)
            .score(BigDecimal.valueOf(score))
            .comment(comment.isBlank() ? null : comment)
            .build();

        try {
            int id = gradeService.addGrade(grade);
            System.out.println("Not eklendi. ID: " + id);
        } catch (Exception e) {
            System.out.println("Hata: " + e.getMessage());
        }
    }

    private void updateGrade() {
        int gId     = ConsoleHelper.readInt("Not ID        : ");
        double score = ConsoleHelper.readDouble("Yeni Puan     : ");
        var comment  = ConsoleHelper.readString("Yeni Yorum    : ");

        boolean ok = gradeService.updateGrade(gId, score, comment.isBlank() ? null : comment);
        System.out.println(ok ? "Not güncellendi." : "Not bulunamadı.");
    }

    private void batchGrades() {
        System.out.println("Enrollment ID'lerini girin (0=bitir):");
        var ids = new java.util.ArrayList<Integer>();
        int id;
        while ((id = ConsoleHelper.readInt("  ID: ")) != 0) {
            ids.add(id);
        }
        var type = ConsoleHelper.readString("Not türü (quiz/homework/project): ");
        var grades = gradeService.createSampleBatchGrades(ids, type);
        gradeService.batchAddGrades(grades);
    }

    // ================================================================
    // RAPOR MENÜSÜ
    // ================================================================
    private void reportMenu() {
        int choice;
        do {
            ConsoleHelper.printHeader("RAPORLAR");
            System.out.println("  1. Öğrenci Not Ortalamaları");
            System.out.println("  2. Ders Not Ortalamaları");
            System.out.println("  3. En Başarılı 5 Öğrenci");
            System.out.println("  4. Öğretmen-Ders Listesi");
            System.out.println("  0. Geri");
            choice = ConsoleHelper.readInt("Seçiminiz: ");

            switch (choice) {
                case 1 -> studentService.printAverageReport();
                case 2 -> gradeService.printCourseAverages();
                case 3 -> gradeService.printTopStudents(5);
                case 4 -> courseService.printTeacherCourses();
            }
            if (choice != 0) ConsoleHelper.waitForEnter();
        } while (choice != 0);
    }
}
