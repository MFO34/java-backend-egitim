-- ============================================================
-- data.sql — Örnek Başlangıç Verileri
--
-- DML (Data Manipulation Language):
-- INSERT, UPDATE, DELETE → Veri işleme komutları
-- ============================================================

-- ============================================================
-- ÖĞRETMENLER
-- ============================================================
-- INSERT INTO tablo (kolon1, kolon2) VALUES (değer1, değer2)
-- id alanını yazmıyoruz çünkü SERIAL otomatik üretiyor
INSERT INTO teachers (first_name, last_name, email, department, salary) VALUES
    ('Ahmet',   'Yıldız',  'ahmet.yildiz@okul.edu',   'Bilgisayar Mühendisliği', 15000.00),
    ('Fatma',   'Şahin',   'fatma.sahin@okul.edu',    'Matematik',               12000.00),
    ('Mehmet',  'Arslan',  'mehmet.arslan@okul.edu',  'Fizik',                   13500.00),
    ('Zeynep',  'Çelik',   'zeynep.celik@okul.edu',   'Bilgisayar Mühendisliği', 16000.00),
    ('Mustafa', 'Kılıç',   'mustafa.kilic@okul.edu',  'İngilizce',               11000.00);

-- ============================================================
-- DERSLER
-- ============================================================
INSERT INTO courses (course_code, course_name, teacher_id, credits, description, capacity) VALUES
    ('CS101', 'Programlamaya Giriş',       1, 4, 'Java ile temel programlama kavramları', 40),
    ('CS201', 'Veri Yapıları',             1, 4, 'Array, LinkedList, Tree, Graph',         35),
    ('CS301', 'Veritabanı Sistemleri',     4, 3, 'SQL, JDBC, ORM kavramları',              30),
    ('CS401', 'Yazılım Mühendisliği',      4, 3, 'Design patterns, SOLID prensipleri',     25),
    ('MATH101','Diferansiyel Hesap',       2, 4, 'Türev ve integral hesabı',               45),
    ('MATH201','Lineer Cebir',             2, 3, 'Matrisler, vektörler, lineer denklemler',40),
    ('PHYS101','Fizik I',                  3, 4, 'Mekanik ve termodinamik',                50),
    ('ENG101', 'Akademik İngilizce',       5, 2, 'Akademik yazma ve sunum becerileri',     35);

-- ============================================================
-- ÖĞRENCİLER
-- ============================================================
INSERT INTO students (first_name, last_name, email, birth_date, national_id, grade_level) VALUES
    ('Ali',     'Yılmaz',  'ali.yilmaz@ogrenci.edu',   '2003-05-15', '12345678901', 2),
    ('Ayşe',    'Kaya',    'ayse.kaya@ogrenci.edu',    '2002-08-22', '23456789012', 3),
    ('Mehmet',  'Demir',   'mehmet.demir@ogrenci.edu', '2004-01-10', '34567890123', 1),
    ('Fatma',   'Çelik',   'fatma.celik@ogrenci.edu',  '2003-11-30', '45678901234', 2),
    ('Can',     'Arslan',  'can.arslan@ogrenci.edu',   '2002-06-05', '56789012345', 3),
    ('Elif',    'Şahin',   'elif.sahin@ogrenci.edu',   '2004-03-18', '67890123456', 1),
    ('Emre',    'Kurt',    'emre.kurt@ogrenci.edu',    '2003-09-25', '78901234567', 2),
    ('Selin',   'Aydın',   'selin.aydin@ogrenci.edu',  '2002-12-07', '89012345678', 3),
    ('Burak',   'Doğan',   'burak.dogan@ogrenci.edu',  '2004-04-20', '90123456789', 1),
    ('Zeynep',  'Polat',   'zeynep.polat@ogrenci.edu', '2003-07-14', '01234567890', 2);

-- ============================================================
-- DERS KAYITLARI (ENROLLMENTS)
-- ============================================================
-- Öğrencileri derslere kaydet
INSERT INTO enrollments (student_id, course_id, status) VALUES
    -- Ali (id=1): 3 derse kayıtlı
    (1, 1, 'active'),  -- CS101
    (1, 2, 'active'),  -- CS201
    (1, 5, 'active'),  -- MATH101

    -- Ayşe (id=2): 3 derse kayıtlı
    (2, 1, 'active'),  -- CS101
    (2, 3, 'active'),  -- CS301
    (2, 6, 'active'),  -- MATH201

    -- Mehmet (id=3): 2 derse kayıtlı
    (3, 1, 'active'),  -- CS101
    (3, 7, 'active'),  -- PHYS101

    -- Fatma (id=4): 3 derse kayıtlı
    (4, 2, 'active'),  -- CS201
    (4, 3, 'active'),  -- CS301
    (4, 8, 'active'),  -- ENG101

    -- Can (id=5): 2 derse kayıtlı
    (5, 4, 'active'),  -- CS401
    (5, 6, 'active'),  -- MATH201

    -- Elif (id=6): 2 derse kayıtlı
    (6, 1, 'active'),  -- CS101
    (6, 8, 'active'),  -- ENG101

    -- Emre (id=7): 2 derse kayıtlı
    (7, 2, 'active'),  -- CS201
    (7, 5, 'active'),  -- MATH101

    -- Selin (id=8): 3 derse kayıtlı
    (8, 3, 'active'),  -- CS301
    (8, 4, 'active'),  -- CS401
    (8, 6, 'active'),  -- MATH201

    -- Burak (id=9): 2 derse kayıtlı
    (9, 1, 'active'),  -- CS101
    (9, 7, 'active'),  -- PHYS101

    -- Zeynep (id=10): 2 derse kayıtlı
    (10, 5, 'active'), -- MATH101
    (10, 8, 'active'); -- ENG101

-- ============================================================
-- NOTLAR (GRADES)
-- ============================================================
-- enrollment_id'ye göre not giriyoruz
-- enrollment sırasına göre id'ler: 1=Ali/CS101, 2=Ali/CS201, vb.
INSERT INTO grades (enrollment_id, grade_type, score, comment) VALUES
    -- Ali - CS101 (enrollment 1)
    (1, 'midterm', 78.5, 'İyi performans'),
    (1, 'final',   82.0, 'Gelişme gösterdi'),

    -- Ali - CS201 (enrollment 2)
    (2, 'midterm', 65.0, NULL),
    (2, 'final',   70.0, NULL),

    -- Ali - MATH101 (enrollment 3)
    (3, 'midterm', 90.0, 'Çok başarılı'),
    (3, 'final',   88.5, 'Mükemmel'),

    -- Ayşe - CS101 (enrollment 4)
    (4, 'midterm', 95.0, 'Sınıfın en iyisi'),
    (4, 'final',   92.0, 'Olağanüstü'),

    -- Ayşe - CS301 (enrollment 5)
    (5, 'midterm', 88.0, NULL),
    (5, 'final',   91.0, NULL),

    -- Mehmet - CS101 (enrollment 7)
    (7, 'midterm', 55.0, 'Daha fazla çalışmalı'),
    (7, 'final',   62.0, 'Gelişme var'),

    -- Fatma - CS201 (enrollment 9)
    (9, 'midterm', 72.0, NULL),
    (9, 'final',   75.5, NULL),

    -- Can - CS401 (enrollment 11)
    (11, 'midterm', 84.0, NULL),
    (11, 'final',   87.0, 'Proje harika'),

    -- Selin - CS301 (enrollment 18)
    (18, 'midterm', 93.0, NULL),
    (18, 'final',   96.0, 'Üstün başarı'),

    -- Burak - CS101 (enrollment 20)
    (20, 'midterm', 48.0, 'Yeterli değil'),
    (20, 'final',   52.0, 'Geçti');
