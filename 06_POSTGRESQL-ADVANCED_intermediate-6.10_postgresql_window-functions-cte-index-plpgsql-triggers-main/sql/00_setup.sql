-- ============================================================
-- 00_SETUP — Eğitim veritabanı şeması ve örnek veriler
-- ============================================================

-- Şema oluştur
CREATE SCHEMA IF NOT EXISTS hr;
CREATE SCHEMA IF NOT EXISTS sales;

-- ============================================================
-- HR Şeması
-- ============================================================
CREATE TABLE hr.departments (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(100)
);

CREATE TABLE hr.employees (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) UNIQUE NOT NULL,
    department_id INTEGER REFERENCES hr.departments(id),
    manager_id    INTEGER REFERENCES hr.employees(id),
    salary        NUMERIC(10,2) NOT NULL,
    hire_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    is_active     BOOLEAN DEFAULT TRUE
);

-- ============================================================
-- Sales Şeması
-- ============================================================
CREATE TABLE sales.products (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    category   VARCHAR(100),
    price      NUMERIC(10,2) NOT NULL,
    stock      INTEGER DEFAULT 0
);

CREATE TABLE sales.orders (
    id           SERIAL PRIMARY KEY,
    customer_id  INTEGER NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    status       VARCHAR(50) DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE sales.order_items (
    id         SERIAL PRIMARY KEY,
    order_id   INTEGER REFERENCES sales.orders(id),
    product_id INTEGER REFERENCES sales.products(id),
    quantity   INTEGER NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL
);

-- ============================================================
-- Örnek Veriler
-- ============================================================
INSERT INTO hr.departments (name, city) VALUES
    ('Mühendislik', 'İstanbul'),
    ('Pazarlama',   'Ankara'),
    ('Finans',      'İstanbul'),
    ('İK',          'İzmir');

INSERT INTO hr.employees (name, email, department_id, manager_id, salary, hire_date) VALUES
    ('Ahmet Yılmaz',  'ahmet@co.com',  1, NULL, 85000, '2020-01-15'),
    ('Ayşe Kaya',     'ayse@co.com',   1, 1,    72000, '2020-03-20'),
    ('Mehmet Demir',  'mehmet@co.com', 1, 1,    68000, '2021-06-01'),
    ('Fatma Çelik',   'fatma@co.com',  2, NULL, 78000, '2019-11-10'),
    ('Ali Şahin',     'ali@co.com',    2, 4,    55000, '2022-02-14'),
    ('Zeynep Arslan', 'zeynep@co.com', 3, NULL, 92000, '2018-05-20'),
    ('Can Öztürk',    'can@co.com',    3, 6,    74000, '2020-09-01'),
    ('Selin Aydın',   'selin@co.com',  4, NULL, 61000, '2021-12-01');

INSERT INTO sales.products (name, category, price, stock) VALUES
    ('MacBook Pro 14"', 'Laptop',  45000, 25),
    ('iPhone 15 Pro',   'Telefon', 38000, 50),
    ('iPad Air',        'Tablet',  22000, 30),
    ('AirPods Pro',     'Aksesuar', 4500, 100),
    ('Dell XPS 15',     'Laptop',  42000, 15),
    ('Samsung S24',     'Telefon', 32000, 60);

INSERT INTO sales.orders (customer_id, total_amount, status, created_at) VALUES
    (1, 83000, 'COMPLETED', '2024-01-15 10:30:00'),
    (2, 38000, 'COMPLETED', '2024-01-20 14:15:00'),
    (1, 22000, 'SHIPPED',   '2024-02-01 09:00:00'),
    (3, 49500, 'PENDING',   '2024-02-10 16:45:00'),
    (2, 87000, 'COMPLETED', '2024-02-15 11:20:00'),
    (4, 4500,  'COMPLETED', '2024-03-01 08:30:00');

INSERT INTO sales.order_items (order_id, product_id, quantity, unit_price) VALUES
    (1, 1, 1, 45000), (1, 2, 1, 38000),
    (2, 2, 1, 38000),
    (3, 3, 1, 22000),
    (4, 5, 1, 42000), (4, 4, 1,  4500), (4, 4, 1, 3000),
    (5, 1, 1, 45000), (5, 5, 1, 42000),
    (6, 4, 1, 4500);
