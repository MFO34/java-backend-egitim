package com.ogrenci.service;

import com.ogrenci.dao.StudentDAO;
import com.ogrenci.exception.StudentNotFoundException;
import com.ogrenci.model.QueryResult;
import com.ogrenci.model.Student;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ÖĞRENCİ İŞ MANTIĞI KATMANI (Service Layer)
 * =============================================
 *
 * Katmanlı Mimari (Layered Architecture):
 *   Controller → Service → DAO → DB
 *   Her katmanın tek sorumluluğu vardır (Single Responsibility).
 *   Service: İş kuralları burada — doğrulama, dönüşüm, hata yönetimi.
 *   DAO: Sadece SQL — iş mantığı bilmez.
 *   Controller: HTTP — iş mantığı bilmez.
 *
 * Neden Service katmanı gerekli?
 *   Controller direkt DAO'yu çağırsaydı:
 *   - İş kuralları controller'a yayılırdı (test zor, tekrar eden kod)
 *   - DAO değişince controller değişmek zorunda kalırdı (tight coupling)
 *   Service: Controller ve DAO arasında bağımsız bir katman.
 *
 * Sealed Class + Pattern Matching (Java 21):
 *   addStudent() → QueryResult<Student> döner.
 *   QueryResult: sealed interface → Success | Failure.
 *   Çağıran taraf: switch(result) ile her durumu ayrı işler.
 *   Avantaj: null kontrolü veya exception yerine tip güvenli sonuç.
 *
 * Stream API:
 *   Java 8+ fonksiyonel programlama.
 *   filter/map/collect: Koleksiyon üzerinde zincirleme dönüşümler.
 *   Collectors.groupingBy: SQL GROUP BY eşdeğeri.
 */
public class StudentService {

    // DAO: Veritabanı işlemleri için — constructor injection
    private final StudentDAO studentDAO;

    /**
     * Constructor injection: Test sırasında mock DAO geçilebilir.
     * Field injection (@Autowired) yerine tercih edilir: final + immutable.
     */
    public StudentService(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    /**
     * Yeni öğrenci ekler.
     *
     * Neden exception yerine QueryResult döner?
     *   Exception: Kontrol akışı için exception kullanmak "exception as control flow" — kötü pratik.
     *   QueryResult: Başarı ve hata durumları tip sisteminde ifade edilir.
     *   Çağıran taraf switch(result) ile Success/Failure durumlarını ayrı işler.
     *
     *   Örnek kullanım:
     *   var result = service.addStudent(student);
     *   switch (result) {
     *     case QueryResult.Success<Student> s → s.data().fullName() göster
     *     case QueryResult.Failure<Student> f → f.message() göster hata
     *   }
     *
     * IllegalArgumentException: DAO seviyesinde doğrulama hatası.
     *   (isim boş, email format hatalı vb.)
     *   Failure döner — exception yukarı fırlatılmaz.
     */
    public QueryResult<Student> addStudent(Student student) {
        try {
            int id = studentDAO.insert(student);

            // Eklenen öğrenciyi DB'den geri getir (generated ID ile)
            Optional<Student> saved = studentDAO.findById(id);

            // Sealed interface pattern matching: başarılı mı, değil mi?
            return saved.isPresent()
                ? QueryResult.success(saved.get())
                : QueryResult.failure("Öğrenci eklendi ama getirilemedi.");

        } catch (IllegalArgumentException e) {
            // Doğrulama hatası (isim boş, email geçersiz)
            return QueryResult.failure("Doğrulama hatası: " + e.getMessage(), e);
        } catch (Exception e) {
            // DB hatası (bağlantı sorunu, unique constraint ihlali vb.)
            return QueryResult.failure("Öğrenci eklenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * ID ile öğrenci getirir.
     *
     * Optional → orElseThrow: Bulunamazsa StudentNotFoundException fırlatır.
     * Global exception handler bu exception'ı yakalar → 404 Not Found döner.
     *
     * Neden null dönmüyor?
     *   null dönmek → çağıran NullPointerException riski.
     *   Optional.orElseThrow → açık hata, tip güvenli.
     */
    public Student getStudentById(int id) {
        return studentDAO.findById(id)
            .orElseThrow(() -> new StudentNotFoundException(id));
    }

    /** Tüm öğrencileri listeler. */
    public List<Student> getAllStudents() {
        return studentDAO.findAll();
    }

    /**
     * İsme göre öğrenci arar.
     *
     * Guard clause: keyword null/boş ise tüm öğrencileri döner.
     * Erken return — iç içe if-else'ten kaçınmak için.
     * DB'ye "%" gibi LIKE sorgusu atmak yerine: null keyword → findAll daha temiz.
     */
    public List<Student> searchStudents(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllStudents(); // Arama yoksa tüm liste
        }
        return studentDAO.searchByName(keyword);
    }

    /**
     * Öğrencileri sınıf düzeyine göre gruplandırır ve yazdırır.
     *
     * Collectors.groupingBy — SQL GROUP BY eşdeğeri:
     *   students.stream()
     *     .collect(groupingBy(Student::gradeLevel))
     *   → Map<Integer, List<Student>>
     *   Key: sınıf numarası (1, 2, 3, 4)
     *   Value: o sınıftaki öğrenci listesi
     *
     * entrySet().stream().sorted(): Map sıralanmaz — stream ile sırala.
     *   gradeLevel 1, 2, 3, 4... sırasıyla yazdır.
     *
     * printf formatı:
     *   %-25s → sola hizalı, 25 karakter → tablo düzeni
     *   %s → sağa hizalı string
     */
    public void printStudentsByGrade() {
        var students = studentDAO.findAll();

        // groupingBy: sınıf numarası → öğrenci listesi eşlemesi
        var grouped = students.stream()
            .collect(Collectors.groupingBy(Student::gradeLevel));

        // Map entry'lerini sınıf numarasına göre sırala, yazdır
        grouped.entrySet().stream()
            .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
            .forEach(entry -> {
                System.out.println("\n--- " + entry.getKey() + ". Sınıf ---");
                entry.getValue().forEach(s ->
                    System.out.printf("  %-25s %s%n", s.fullName(), s.email())
                );
            });
    }

    /**
     * Öğrenci bilgilerini günceller.
     *
     * Neden önce findById yapılır?
     *   DAO.update() dönen satır sayısından var/yok anlaşılabilir.
     *   Ama önce findById: "bulunamadı" vs "güncelleme başarısız" ayrımı net.
     *   StudentNotFoundException: 404 Not Found → çağırana anlamlı hata.
     *   DAO update başarısızlığı: 500 Internal Error → farklı durum.
     */
    public boolean updateStudent(Student student) {
        // Öğrenci var mı kontrol et — yoksa anlamlı hata
        studentDAO.findById(student.id())
            .orElseThrow(() -> new StudentNotFoundException(student.id()));
        return studentDAO.update(student);
    }

    /**
     * Öğrenciyi pasif yapar (soft delete).
     *
     * Neden hard delete değil soft delete?
     *   Silinmiş öğrencinin not geçmişi, dönem kayıtları FK referansı tutar.
     *   DB'den silmek: FK ihlali veya cascade → veri kaybı riski.
     *   isActive=false → öğrenci listelerde görünmez, geçmişi korunur.
     *   KVKK talebi: Ayrı anonymize() metodu ile kişisel veriler silinir.
     */
    public boolean deleteStudent(int id) {
        studentDAO.findById(id)
            .orElseThrow(() -> new StudentNotFoundException(id));
        return studentDAO.softDelete(id); // UPDATE students SET is_active=false WHERE id=?
    }

    /**
     * Öğrenci not ortalaması raporunu yazdırır.
     *
     * getStudentAverages(): DB'de aggregate query çalıştırır.
     *   SELECT s.name, c.name, AVG(g.score), MAX(g.score), MIN(g.score)
     *   FROM students s JOIN grades g JOIN courses c
     *   GROUP BY s.id, c.id
     *
     * List<Object[]> dönüş tipi neden?
     *   Multiple JOIN + GROUP BY sonucu entity'ye maplenemiyor.
     *   Object[] → her sütun bir eleman: [isim, ders, ortalama, max, min]
     *   Gerçek uygulamada DTO projeksiyonu veya record ile daha temiz yazılır.
     */
    public void printAverageReport() {
        var data = studentDAO.getStudentAverages();

        // Tablo başlığı — printf ile hizalı format
        System.out.printf("%-25s | %-5s | %-8s | %-8s | %-8s%n",
            "Öğrenci", "Ders", "Ortalama", "En Yüksek", "En Düşük");
        System.out.println("-".repeat(65));

        // Object[] → her sütun index ile erişilir
        data.forEach(row ->
            System.out.printf("%-25s | %-5s | %-8s | %-8s | %-8s%n",
                row[0], row[1], row[2], row[3], row[4])
        );
    }

    /**
     * Toplu öğrenci ekler — Batch Insert.
     *
     * batchInsert(): JDBC PreparedStatement.executeBatch()
     *   N öğrenci için N ayrı INSERT yerine tek bir batch.
     *   Veritabanı round-trip: N → 1 (büyük performans kazancı).
     *
     * int[] results: Her satır için etkilenen kayıt sayısı.
     *   results[i] > 0 → başarılı
     *   results[i] = Statement.EXECUTE_FAILED → başarısız
     *
     * Arrays.stream(results).filter(r -> r > 0).count():
     *   Kaç ekleme başarılı sayısını hesaplar.
     */
    public void batchAddStudents(List<Student> students) {
        int[] results = studentDAO.batchInsert(students);

        // Başarılı ekleme sayısını hesapla (r > 0 → 1 satır etkilendi)
        long success = java.util.Arrays.stream(results)
            .filter(r -> r > 0)
            .count();

        System.out.printf("Toplu ekleme: %d/%d başarılı.%n", success, results.length);
    }
}
