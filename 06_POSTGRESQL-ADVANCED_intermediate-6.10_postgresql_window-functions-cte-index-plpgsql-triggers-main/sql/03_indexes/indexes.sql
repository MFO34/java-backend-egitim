-- ============================================================
-- 03 — INDEXES — Performans optimizasyonu
-- ============================================================

-- ----------------------------------------------------------------
-- Index Türleri
-- B-Tree   → varsayılan, sıralı veri, =, <, >, BETWEEN, LIKE 'abc%'
-- Hash     → yalnızca = karşılaştırması
-- GIN      → JSONB, dizi, tam metin arama
-- GiST     → geometrik veri, tam metin, range tipler
-- BRIN     → çok büyük, fiziksel sıralı tablolar (log tabloları)
-- ----------------------------------------------------------------

-- B-Tree Index
CREATE INDEX idx_employees_salary     ON hr.employees(salary);
CREATE INDEX idx_employees_dept       ON hr.employees(department_id);
CREATE INDEX idx_orders_created_at    ON sales.orders(created_at);
CREATE INDEX idx_orders_status        ON sales.orders(status);
CREATE INDEX idx_order_items_order_id ON sales.order_items(order_id);

-- Composite Index — çok kolonlu sorgu
CREATE INDEX idx_employees_dept_salary ON hr.employees(department_id, salary DESC);

-- Partial Index — WHERE koşulu ile (daha küçük, daha hızlı)
CREATE INDEX idx_orders_pending ON sales.orders(created_at)
WHERE status = 'PENDING';

-- Unique Index
CREATE UNIQUE INDEX idx_employees_email ON hr.employees(email);

-- Expression Index — hesaplanmış değer üzerinde
CREATE INDEX idx_employees_name_lower ON hr.employees(LOWER(name));

-- GIN — JSONB ve tam metin için
CREATE INDEX idx_products_name_fts ON sales.products USING GIN(to_tsvector('turkish', name));

-- ----------------------------------------------------------------
-- EXPLAIN ANALYZE — Sorgu planını oku
-- Mülakat: "Yavaş sorguyu nasıl optimize edersin?"
-- ----------------------------------------------------------------
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
SELECT e.name, e.salary, d.name AS dept
FROM hr.employees e
JOIN hr.departments d ON d.id = e.department_id
WHERE e.salary > 70000
ORDER BY e.salary DESC;

-- ----------------------------------------------------------------
-- Index kullanımı kontrolü
-- ----------------------------------------------------------------
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan    AS index_scans,
    idx_tup_read AS tuples_read,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Hiç kullanılmayan indexler (silinmesi gerekenler)
SELECT indexrelid::regclass AS index_name, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0;

-- ----------------------------------------------------------------
-- Index Bloat — Şişmiş indexleri bul
-- ----------------------------------------------------------------
SELECT
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(oid)) AS total_size,
    pg_size_pretty(pg_relation_size(oid))        AS table_size
FROM pg_class
WHERE relkind = 'r'
ORDER BY pg_total_relation_size(oid) DESC
LIMIT 10;

-- REINDEX — Index yeniden oluştur
-- REINDEX INDEX CONCURRENTLY idx_employees_salary;

-- ----------------------------------------------------------------
-- Covering Index (INCLUDE) — Index-only scan
-- ----------------------------------------------------------------
CREATE INDEX idx_employees_covering ON hr.employees(department_id)
INCLUDE (name, salary);
-- Bu index; SELECT name, salary FROM employees WHERE department_id = 1
-- sorgusunu table access olmadan karşılar

-- ----------------------------------------------------------------
-- Index Stratejisi Özeti (mülakat notları)
-- ----------------------------------------------------------------
-- ✓ Yüksek cardinality kolonlara index ekle (email, salary)
-- ✓ WHERE koşulunda sık kullanılan kolonlar
-- ✓ JOIN koşullarındaki foreign key'ler
-- ✓ ORDER BY kolonları (sıralama için)
-- ✗ Küçük tablolara index ekleme (sequential scan daha hızlı)
-- ✗ Yazma ağır tablolarda fazla index (INSERT/UPDATE yavaşlar)
-- ✗ Low cardinality kolonlar (boolean, status — partial index tercih et)
