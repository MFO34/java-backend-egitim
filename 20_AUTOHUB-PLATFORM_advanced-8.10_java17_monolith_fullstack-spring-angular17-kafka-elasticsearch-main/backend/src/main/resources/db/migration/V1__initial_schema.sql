-- AutoHub Veritabanı Şema Migration - V1 (İlk Sürüm)
-- Flyway bu dosyayı V1 olarak tanır ve bir kez uygular
-- Tüm temel tablolar bu migration'da oluşturulur

-- =====================================================
-- ROLLER TABLOSU
-- Sistemdeki kullanıcı rollerini tanımlar
-- =====================================================
CREATE TABLE IF NOT EXISTS roles (
    id              BIGSERIAL PRIMARY KEY,          -- Otomatik artan birincil anahtar
    name            VARCHAR(50) NOT NULL UNIQUE,     -- Rol adı (ADMIN, MANAGER vs.)
    description     VARCHAR(200),                    -- Rol açıklaması
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,   -- Rol aktif mi?
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,  -- Soft delete flag
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),-- Oluşturma zamanı
    updated_at      TIMESTAMP,                       -- Son güncelleme zamanı
    created_by      VARCHAR(100),                    -- Kim oluşturdu
    updated_by      VARCHAR(100)                     -- Kim güncelledi
);

-- Varsayılan rolleri ekle
INSERT INTO roles (name, description) VALUES
    ('ADMIN',    'Sistem yöneticisi - tüm işlemlere erişim'),
    ('MANAGER',  'Şube müdürü - araç ve müşteri yönetimi'),
    ('EMPLOYEE', 'Çalışan - günlük işlemleri gerçekleştirir'),
    ('CUSTOMER', 'Müşteri - sadece kendi hesabını yönetir');

-- =====================================================
-- KULLANICILAR TABLOSU
-- Sisteme giriş yapan kullanıcılar
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL PRIMARY KEY,
    username                VARCHAR(50) NOT NULL UNIQUE,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,          -- BCrypt hash
    first_name              VARCHAR(50),
    last_name               VARCHAR(50),
    account_locked          BOOLEAN NOT NULL DEFAULT FALSE, -- Hesap kilitli mi?
    account_expired         BOOLEAN NOT NULL DEFAULT FALSE, -- Hesap süresi doldu mu?
    credentials_expired     BOOLEAN NOT NULL DEFAULT FALSE, -- Şifre süresi doldu mu?
    failed_attempts         INTEGER NOT NULL DEFAULT 0,     -- Başarısız giriş sayısı
    last_login_at           TIMESTAMP,                      -- Son giriş zamanı
    password_reset_token    VARCHAR(100),                   -- Şifre sıfırlama tokeni
    reset_token_expiry      TIMESTAMP,                      -- Token bitiş zamanı
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

-- Kullanıcı-Rol ara tablosu (Many-to-Many ilişki)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id     BIGINT NOT NULL REFERENCES users(id),
    role_id     BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)  -- Bileşik birincil anahtar - duplicate önler
);

-- Varsayılan admin kullanıcısı oluştur
-- Şifre: Admin@123! (BCrypt hash)
INSERT INTO users (username, email, password, first_name, last_name) VALUES
    ('admin', 'admin@autohub.com',
     '$2a$12$4bGrWlFnELEdMhUiUvSL8e3KT8e8GrWKdT8J5KpXzJ3RVKG5LFEZS',
     'Sistem', 'Yöneticisi');

-- Admin kullanıcısına ADMIN rolü ver
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- =====================================================
-- MARKALAR TABLOSU
-- Araç üreticileri (Toyota, BMW vs.)
-- =====================================================
CREATE TABLE IF NOT EXISTS brands (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,   -- Marka adı
    country         VARCHAR(100),                   -- Köken ülke
    logo_url        VARCHAR(500),                   -- Logo resim URL'i
    founded_year    INTEGER,                        -- Kuruluş yılı
    description     TEXT,                           -- Marka açıklaması
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- Index'ler performans için
CREATE INDEX idx_brand_name ON brands(name);
CREATE INDEX idx_brand_country ON brands(country);

-- Örnek marka verileri
INSERT INTO brands (name, country, founded_year, description) VALUES
    ('Toyota',    'Japonya',  1937, 'Dünyanın en büyük otomobil üreticisi'),
    ('BMW',       'Almanya',  1916, 'Bavyera Motor İşleri - lüks Alman otomobilleri'),
    ('Mercedes',  'Almanya',  1926, 'Mercedes-Benz - lüks ve ticari araçlar'),
    ('Honda',     'Japonya',  1948, 'Honda Motor Co. - otomobil ve motosiklet'),
    ('Ford',      'ABD',      1903, 'Ford Motor Company - Amerikan otomobil üreticisi'),
    ('Renault',   'Fransa',   1899, 'Renault - Fransız otomobil üreticisi'),
    ('Volkswagen','Almanya',  1937, 'Volkswagen - Halk arabası'),
    ('Audi',      'Almanya',  1909, 'Audi AG - premium Alman otomobil markası'),
    ('Hyundai',   'Güney Kore', 1967, 'Hyundai Motor - Güney Kore otomobil üreticisi'),
    ('Kia',       'Güney Kore', 1944, 'Kia Corporation - Güney Kore otomobil markası');

-- =====================================================
-- ARAÇ MODELLERİ TABLOSU
-- Her markanın sahip olduğu model bilgileri
-- =====================================================
CREATE TABLE IF NOT EXISTS vehicle_models (
    id                  BIGSERIAL PRIMARY KEY,
    brand_id            BIGINT NOT NULL REFERENCES brands(id),
    name                VARCHAR(100) NOT NULL,
    vehicle_type        VARCHAR(50) NOT NULL,   -- SEDAN, SUV, HATCHBACK vs.
    model_year          INTEGER,
    engine_capacity     INTEGER,                -- Motor hacmi (cc)
    fuel_type           VARCHAR(30),            -- GASOLINE, DIESEL, ELECTRIC vs.
    transmission_type   VARCHAR(30),            -- MANUAL, AUTOMATIC
    seat_count          INTEGER,
    manual_url          VARCHAR(500),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    UNIQUE(brand_id, name)  -- Aynı marka altında aynı isimde model olamaz
);

CREATE INDEX idx_model_brand_name ON vehicle_models(brand_id, name);
CREATE INDEX idx_model_year ON vehicle_models(model_year);
CREATE INDEX idx_model_type ON vehicle_models(vehicle_type);

-- =====================================================
-- ARAÇLAR TABLOSU
-- Stokta bulunan araç kayıtları
-- =====================================================
CREATE TABLE IF NOT EXISTS vehicles (
    id                      BIGSERIAL PRIMARY KEY,
    model_id                BIGINT NOT NULL REFERENCES vehicle_models(id),
    license_plate           VARCHAR(15) NOT NULL UNIQUE,    -- Türkiye plaka formatı
    year                    INTEGER NOT NULL,
    color                   VARCHAR(50),
    mileage                 INTEGER DEFAULT 0,
    price                   DECIMAL(12, 2) NOT NULL,        -- Satış fiyatı
    daily_rate              DECIMAL(10, 2),                 -- Günlük kiralama ücreti
    vin_number              VARCHAR(17) UNIQUE,             -- Vehicle Identification Number
    engine_number           VARCHAR(50) UNIQUE,
    description             TEXT,
    status                  VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    last_inspection_date    DATE,                           -- Son muayene tarihi
    insurance_expiry_date   DATE,                           -- Sigorta bitiş tarihi
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

-- Araç fotoğrafları (One-to-Many element collection)
CREATE TABLE IF NOT EXISTS vehicle_images (
    vehicle_id  BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    image_url   VARCHAR(500) NOT NULL
);

-- Araç özellikleri (One-to-Many element collection)
CREATE TABLE IF NOT EXISTS vehicle_features (
    vehicle_id  BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    feature     VARCHAR(100) NOT NULL
);

-- Performans index'leri
CREATE INDEX idx_vehicle_plate ON vehicles(license_plate);
CREATE INDEX idx_vehicle_status ON vehicles(status);
CREATE INDEX idx_vehicle_model_year ON vehicles(model_id, year);
CREATE INDEX idx_vehicle_price ON vehicles(price);

-- =====================================================
-- MÜŞTERİLER TABLOSU
-- Kiralama ve satış yapılan müşteriler
-- =====================================================
CREATE TABLE IF NOT EXISTS customers (
    id                      BIGSERIAL PRIMARY KEY,
    tc_identity_number      VARCHAR(11) NOT NULL UNIQUE,    -- T.C. Kimlik No
    first_name              VARCHAR(50) NOT NULL,
    last_name               VARCHAR(50) NOT NULL,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    phone_number            VARCHAR(15),
    birth_date              DATE,
    driving_license_number  VARCHAR(20),
    license_expiry_date     DATE,
    address                 TEXT,
    city                    VARCHAR(100),
    postal_code             VARCHAR(5),
    customer_type           VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
    company_name            VARCHAR(200),
    tax_number              VARCHAR(11),
    notes                   TEXT,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

CREATE INDEX idx_customer_tc ON customers(tc_identity_number);
CREATE INDEX idx_customer_email ON customers(email);
CREATE INDEX idx_customer_phone ON customers(phone_number);
CREATE INDEX idx_customer_type ON customers(customer_type);

-- =====================================================
-- İŞLEMLER TABLOSU
-- Kiralama ve satış işlem kayıtları
-- =====================================================
CREATE TABLE IF NOT EXISTS transactions (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_number  VARCHAR(30) NOT NULL UNIQUE,    -- TRX-20240115-0001 formatı
    transaction_type    VARCHAR(20) NOT NULL,           -- RENTAL veya SALE
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    vehicle_id          BIGINT NOT NULL REFERENCES vehicles(id),
    customer_id         BIGINT NOT NULL REFERENCES customers(id),
    created_by_user_id  BIGINT REFERENCES users(id),
    transaction_date    TIMESTAMP NOT NULL DEFAULT NOW(),
    rental_start_date   DATE,                          -- Kiralama başlangıcı
    rental_end_date     DATE,                          -- Tahmini iade tarihi
    actual_return_date  DATE,                          -- Gerçek iade tarihi
    pickup_mileage      INTEGER,                       -- Teslim alınan km
    return_mileage      INTEGER,                       -- İade edilen km
    base_amount         DECIMAL(12, 2) NOT NULL,
    discount_amount     DECIMAL(10, 2) DEFAULT 0,
    tax_amount          DECIMAL(10, 2),
    additional_charges  DECIMAL(10, 2) DEFAULT 0,
    total_amount        DECIMAL(12, 2),
    payment_method      VARCHAR(30),
    notes               TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX idx_transaction_number ON transactions(transaction_number);
CREATE INDEX idx_transaction_customer ON transactions(customer_id);
CREATE INDEX idx_transaction_vehicle ON transactions(vehicle_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_transaction_type_status ON transactions(transaction_type, status);

-- Migration tamamlandı mesajı
COMMENT ON TABLE vehicles IS 'AutoHub araç stok tablosu';
COMMENT ON TABLE transactions IS 'Kiralama ve satış işlem kayıtları';
