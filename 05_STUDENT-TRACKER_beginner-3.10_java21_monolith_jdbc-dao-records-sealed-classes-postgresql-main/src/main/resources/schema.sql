-- ============================================================
-- schema.sql — Veritabanı Şeması
--
-- Bu dosya tüm tabloları, kısıtları (constraint) ve
-- indeksleri oluşturur.
--
-- SQL KAVRAMI: DDL (Data Definition Language)
-- CREATE, ALTER, DROP, INDEX → Yapı tanımlama komutları
-- ============================================================

-- Varsa eski tabloları sil (geliştirme ortamı için pratik)
-- CASCADE: bu tabloya bağlı diğer tablolar da silinir
DROP TABLE IF EXISTS grades CASCADE;
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS teachers CASCADE;
DROP TABLE IF EXISTS students CASCADE;

-- ============================================================
-- STUDENTS TABLOSU — Öğrenci bilgileri
-- ============================================================
CREATE TABLE students (
    -- PRIMARY KEY: Her satırı benzersiz tanımlayan alan
    -- SERIAL: PostgreSQL'in otomatik artan tam sayı tipi (1, 2, 3, ...)
    id          SERIAL PRIMARY KEY,

    -- VARCHAR(n): Maksimum n karakter uzunluğunda metin
    -- NOT NULL: Bu alan boş bırakılamaz
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,

    -- UNIQUE: Bu alanda aynı değer iki kez olamaz
    email       VARCHAR(100) NOT NULL UNIQUE,

    -- DATE: Sadece tarih (gün/ay/yıl), saat bilgisi yok
    birth_date  DATE,

    -- CHAR(11): Tam 11 karakter, TC kimlik için
    national_id CHAR(11)     UNIQUE,

    -- INTEGER: Tam sayı — sınıf/yıl için ideal
    grade_level INTEGER      CHECK (grade_level BETWEEN 1 AND 4),

    -- BOOLEAN: true/false — DEFAULT ile varsayılan değer
    is_active   BOOLEAN      DEFAULT TRUE,

    -- TIMESTAMP: Tarih + saat bilgisi birlikte
    -- NOW(): PostgreSQL'in şu anki zamanı döndüren fonksiyonu
    created_at  TIMESTAMP    DEFAULT NOW(),
    updated_at  TIMESTAMP    DEFAULT NOW()
);

-- ============================================================
-- TEACHERS TABLOSU — Öğretmen bilgileri
-- ============================================================
CREATE TABLE teachers (
    id          SERIAL       PRIMARY KEY,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,

    -- Branş/uzmanlık alanı
    department  VARCHAR(100),

    -- DECIMAL(p, s): p toplam basamak, s ondalık basamak
    -- Maaş için: 99999.99 gibi değerler tutabilir
    salary      DECIMAL(10, 2),

    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT NOW()
);

-- ============================================================
-- COURSES TABLOSU — Ders bilgileri
-- ============================================================
CREATE TABLE courses (
    id          SERIAL       PRIMARY KEY,

    -- Ders kodu: CS101, MATH201 gibi — benzersiz olmalı
    course_code VARCHAR(10)  NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,

    -- REFERENCES: FOREIGN KEY tanımı
    -- courses.teacher_id → teachers.id'ye bağlıdır
    -- ON DELETE SET NULL: öğretmen silinirse ders sahipsiz kalır (NULL)
    teacher_id  INTEGER      REFERENCES teachers(id) ON DELETE SET NULL,

    -- Kredi değeri: 1-6 arasında olmalı
    credits     INTEGER      NOT NULL CHECK (credits BETWEEN 1 AND 6),

    -- TEXT: Sınırsız uzunlukta metin (VARCHAR'dan farklı)
    description TEXT,

    -- Kontenjan: kaç öğrenci alabilir?
    capacity    INTEGER      DEFAULT 30,

    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT NOW()
);

-- ============================================================
-- ENROLLMENTS TABLOSU — Öğrenci-Ders kaydı (ara tablo)
--
-- Çoka-çok (Many-to-Many) ilişki:
--   Bir öğrenci birden fazla derse girebilir
--   Bir derse birden fazla öğrenci girebilir
-- Bu ilişkiyi kurmak için ara tablo gerekir
-- ============================================================
CREATE TABLE enrollments (
    id          SERIAL    PRIMARY KEY,

    -- Her iki tabloya da foreign key
    student_id  INTEGER   NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id   INTEGER   NOT NULL REFERENCES courses(id)  ON DELETE CASCADE,

    -- Kayıt tarihi
    enrolled_at TIMESTAMP DEFAULT NOW(),

    -- Durum: active, dropped, completed
    status      VARCHAR(20) DEFAULT 'active'
                CHECK (status IN ('active', 'dropped', 'completed')),

    -- UNIQUE constraint: aynı öğrenci aynı derse iki kez kayıt olamaz
    CONSTRAINT unique_enrollment UNIQUE (student_id, course_id)
);

-- ============================================================
-- GRADES TABLOSU — Not bilgileri
-- ============================================================
CREATE TABLE grades (
    id            SERIAL      PRIMARY KEY,

    -- Hangi kayda ait not? (enrollment üzerinden)
    enrollment_id INTEGER     NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,

    -- Not tipi: midterm (vize), final, quiz, homework
    grade_type    VARCHAR(20) NOT NULL
                  CHECK (grade_type IN ('midterm', 'final', 'quiz', 'homework', 'project')),

    -- DECIMAL(5,2): 100.00 gibi notlar tutabilir
    score         DECIMAL(5,2) NOT NULL
                  CHECK (score BETWEEN 0 AND 100),

    -- Notun girildiği tarih
    graded_at     TIMESTAMP    DEFAULT NOW(),

    -- Not açıklaması (isteğe bağlı)
    comment       TEXT
);

-- ============================================================
-- INDEX'LER — Sorgu hızlandırma
--
-- INDEX KAVRAMI:
-- Kitabın sonundaki dizin gibi — hızlı arama sağlar
-- JOIN ve WHERE sorgularını dramatik hızlandırır
-- Her index ek disk alanı kullanır (tradeoff)
-- ============================================================

-- Öğrenci soyadına göre arama hızlandır
CREATE INDEX idx_students_last_name ON students(last_name);

-- E-posta araması hızlandır (login gibi işlemler için)
CREATE INDEX idx_students_email ON students(email);

-- Derse göre enrollment arama (JOIN sorgularında kullanılır)
CREATE INDEX idx_enrollments_course ON enrollments(course_id);

-- Öğrenciye göre enrollment arama
CREATE INDEX idx_enrollments_student ON enrollments(student_id);

-- Notları enrollment'a göre hızlı bul
CREATE INDEX idx_grades_enrollment ON grades(enrollment_id);

-- Öğretmen departmanına göre sorgulama
CREATE INDEX idx_teachers_department ON teachers(department);

-- ============================================================
-- TRIGGER FONKSİYONU — updated_at otomatik güncelleme
--
-- TRIGGER: Belirli bir olay olunca otomatik çalışan SQL
-- Her UPDATE işleminde updated_at'i güncellesin
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    -- NEW: güncellenen satırın yeni değerleri
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Students tablosuna trigger bağla
CREATE TRIGGER update_students_updated_at
    BEFORE UPDATE ON students           -- UPDATE'den önce çalış
    FOR EACH ROW                        -- Her satır için
    EXECUTE FUNCTION update_updated_at_column();
