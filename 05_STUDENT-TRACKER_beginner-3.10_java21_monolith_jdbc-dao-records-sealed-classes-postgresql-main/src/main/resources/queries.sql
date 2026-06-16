-- ============================================================
-- queries.sql — Önemli SQL Sorguları ve Açıklamaları
--
-- Bu dosya öğretici amaçlıdır.
-- Uygulamada kullanılan sorgular burada açıklanmıştır.
-- ============================================================

-- ============================================================
-- 1. TEMEL SELECT SORGULARI
-- ============================================================

-- Tüm aktif öğrencileri soyada göre sırala
-- ORDER BY: sıralama  |  ASC: artan (varsayılan)  |  DESC: azalan
SELECT id, first_name, last_name, email, grade_level
FROM students
WHERE is_active = TRUE
ORDER BY last_name ASC, first_name ASC;

-- Belirli sınıftaki öğrenci sayısı
-- GROUP BY: aynı değerleri gruplar  |  COUNT: sayar
SELECT grade_level, COUNT(*) AS ogrenci_sayisi
FROM students
WHERE is_active = TRUE
GROUP BY grade_level
ORDER BY grade_level;

-- ============================================================
-- 2. INNER JOIN — Sadece eşleşen satırlar
-- ============================================================

-- Öğrenci + kayıtlı olduğu dersler (INNER JOIN)
-- Her iki tabloda da eşleşme OLMAK ZORUNDA
SELECT
    s.first_name || ' ' || s.last_name AS ogrenci_adi,  -- || : String birleştirme
    c.course_code,
    c.course_name,
    e.enrolled_at,
    e.status
FROM students s                          -- s: tablo takma adı (alias)
INNER JOIN enrollments e ON s.id = e.student_id
INNER JOIN courses c     ON e.course_id = c.id
WHERE s.is_active = TRUE
ORDER BY s.last_name, c.course_code;

-- ============================================================
-- 3. LEFT JOIN — Sol tablodaki tüm satırlar
-- ============================================================

-- Tüm öğrenciler + dersleri (hiç ders almamış olsa bile)
-- LEFT JOIN: sol tabloda eşleşme olmasa bile satırı dahil et
SELECT
    s.first_name || ' ' || s.last_name AS ogrenci_adi,
    COUNT(e.id) AS ders_sayisi           -- NULL da sayılmaz
FROM students s
LEFT JOIN enrollments e ON s.id = e.student_id AND e.status = 'active'
GROUP BY s.id, s.first_name, s.last_name
ORDER BY ders_sayisi DESC;

-- ============================================================
-- 4. RIGHT JOIN — Sağ tablodaki tüm satırlar
-- ============================================================

-- Tüm dersler + öğretmenleri (öğretmensiz ders olsa bile)
SELECT
    c.course_code,
    c.course_name,
    t.first_name || ' ' || t.last_name AS ogretmen_adi
FROM teachers t
RIGHT JOIN courses c ON t.id = c.teacher_id
ORDER BY c.course_code;

-- ============================================================
-- 5. AGGREGATE FUNCTIONS — Toplu hesaplamalar
-- ============================================================

-- Öğrencinin ders bazında not ortalaması
-- AVG: ortalama  |  ROUND: yuvarla
SELECT
    s.first_name || ' ' || s.last_name AS ogrenci_adi,
    c.course_name,
    AVG(g.score) AS ortalama_not,
    MAX(g.score) AS en_yuksek,
    MIN(g.score) AS en_dusuk,
    COUNT(g.id)  AS not_sayisi
FROM students s
INNER JOIN enrollments e ON s.id = e.student_id
INNER JOIN courses c     ON e.course_id = c.id
INNER JOIN grades g      ON e.id = g.enrollment_id
GROUP BY s.id, s.first_name, s.last_name, c.id, c.course_name
ORDER BY ortalama_not DESC;

-- ============================================================
-- 6. HAVING — Gruplama sonrası filtreleme
--
-- WHERE: gruplama ÖNCESİ filtreler
-- HAVING: gruplama SONRASI filtreler (aggregate kullanabilir)
-- ============================================================

-- Ortalaması 70'in üzerinde olan öğrenciler
SELECT
    s.first_name || ' ' || s.last_name AS ogrenci_adi,
    ROUND(AVG(g.score), 2) AS genel_ortalama
FROM students s
INNER JOIN enrollments e ON s.id = e.student_id
INNER JOIN grades g      ON e.id = g.enrollment_id
GROUP BY s.id, s.first_name, s.last_name
HAVING AVG(g.score) > 70          -- HAVING: aggregate fonksiyon ile filtrele
ORDER BY genel_ortalama DESC;

-- ============================================================
-- 7. SUBQUERY — İç içe sorgular
-- ============================================================

-- En yüksek not ortalamasına sahip öğrenci (subquery ile)
SELECT first_name, last_name, email
FROM students
WHERE id = (
    -- Alt sorgu: en yüksek ortalamaya sahip öğrenci id'sini bul
    SELECT e.student_id
    FROM enrollments e
    INNER JOIN grades g ON e.id = g.enrollment_id
    GROUP BY e.student_id
    ORDER BY AVG(g.score) DESC
    LIMIT 1   -- Sadece ilk satırı al
);

-- Hiç not girilmemiş öğrenciler (NOT EXISTS subquery)
SELECT s.first_name, s.last_name
FROM students s
WHERE NOT EXISTS (
    SELECT 1
    FROM enrollments e
    INNER JOIN grades g ON e.id = g.enrollment_id
    WHERE e.student_id = s.id
);

-- ============================================================
-- 8. WINDOW FUNCTION — Sıralama (PostgreSQL özelliği)
-- ============================================================

-- Öğrencileri nota göre sırala (RANK fonksiyonu)
SELECT
    s.first_name || ' ' || s.last_name AS ogrenci_adi,
    ROUND(AVG(g.score), 2) AS ortalama,
    -- RANK(): aynı değerlere aynı sıra numarası verir
    RANK() OVER (ORDER BY AVG(g.score) DESC) AS sira
FROM students s
INNER JOIN enrollments e ON s.id = e.student_id
INNER JOIN grades g      ON e.id = g.enrollment_id
GROUP BY s.id, s.first_name, s.last_name
ORDER BY sira;

-- ============================================================
-- 9. TRANSACTION ÖRNEĞİ
-- ============================================================

-- Transaction: ya hepsi başarılı olur, ya da hiçbiri olmaz (atomik)
BEGIN;                          -- İşlem başlat

    -- Öğrenci ekle
    INSERT INTO students (first_name, last_name, email, grade_level)
    VALUES ('Test', 'Öğrenci', 'test@test.com', 1);

    -- Aynı öğrenciyi derse kaydet (id'yi öğrenmemiz gerekir)
    -- LASTVAL(): son oluşturulan SERIAL değerini döner
    INSERT INTO enrollments (student_id, course_id)
    VALUES (LASTVAL(), 1);

COMMIT;                         -- Başarılıysa kaydet
-- ROLLBACK;                    -- Hata varsa geri al

-- ============================================================
-- 10. BATCH INSERT — Toplu veri ekleme
-- ============================================================

-- Tek INSERT ile birden fazla satır ekle (daha hızlı)
INSERT INTO grades (enrollment_id, grade_type, score) VALUES
    (1, 'quiz', 85.0),
    (2, 'quiz', 72.0),
    (3, 'quiz', 91.5);

-- ============================================================
-- 11. UPDATE — Güncelleme
-- ============================================================

-- Tek öğrenci güncelleme
UPDATE students
SET first_name = 'Yeni Ad',
    updated_at = NOW()          -- Güncelleme zamanını kaydet
WHERE id = 1;

-- Toplu güncelleme: tüm derslerin kapasitesini artır
UPDATE courses
SET capacity = capacity + 5     -- Mevcut değere 5 ekle
WHERE is_active = TRUE;

-- ============================================================
-- 12. DELETE — Silme
-- ============================================================

-- Soft delete: silmek yerine pasif yap (önerilir!)
UPDATE students SET is_active = FALSE WHERE id = 9;

-- Hard delete: gerçekten sil (CASCADE bağlı kayıtları da siler)
-- DELETE FROM students WHERE id = 9;
