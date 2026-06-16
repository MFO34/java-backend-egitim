-- ============================================================
-- 08 — PERFORMANCE — Sorgu optimizasyonu
-- ============================================================

-- ----------------------------------------------------------------
-- EXPLAIN çıktısını okuma
-- Seq Scan   → tüm tablo taranıyor (index yok veya küçük tablo)
-- Index Scan → index kullanılıyor
-- Index Only Scan → sadece index, tablo okunmuyor (covering index)
-- Bitmap Scan → çok satır dönerken index kullanımı
-- Hash Join  → büyük tablo join'leri
-- Nested Loop → küçük veri set join'leri
-- ----------------------------------------------------------------
EXPLAIN (ANALYZE, BUFFERS)
SELECT e.name, e.salary, d.name AS dept
FROM hr.employees e
JOIN hr.departments d ON d.id = e.department_id
WHERE e.salary > 70000;

-- ----------------------------------------------------------------
-- pg_stat_statements — En yavaş sorgular
-- ----------------------------------------------------------------
-- Extension yüklü olması gerekir: CREATE EXTENSION pg_stat_statements;
SELECT
    query,
    calls,
    ROUND(total_exec_time::NUMERIC, 2) AS total_ms,
    ROUND(mean_exec_time::NUMERIC, 2)  AS avg_ms,
    rows
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- ----------------------------------------------------------------
-- Tablo ve Index boyutları
-- ----------------------------------------------------------------
SELECT
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(oid))   AS total,
    pg_size_pretty(pg_relation_size(oid))          AS table_only,
    pg_size_pretty(pg_indexes_size(oid))           AS indexes
FROM pg_class
WHERE relkind = 'r' AND relname NOT LIKE 'pg_%'
ORDER BY pg_total_relation_size(oid) DESC;

-- ----------------------------------------------------------------
-- Bağlantı yönetimi
-- ----------------------------------------------------------------
SELECT
    state,
    COUNT(*) AS connection_count,
    AVG(EXTRACT(EPOCH FROM (NOW() - state_change)))::INT AS avg_seconds
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY state;

-- Uzun süren sorgular
SELECT
    pid, now() - pg_stat_activity.query_start AS duration,
    query, state
FROM pg_stat_activity
WHERE state != 'idle'
  AND now() - pg_stat_activity.query_start > INTERVAL '5 minutes';

-- ----------------------------------------------------------------
-- Partitioning — Büyük tablolarda performans
-- ----------------------------------------------------------------
CREATE TABLE sales.orders_partitioned (
    id           SERIAL,
    customer_id  INTEGER NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    status       VARCHAR(50),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Yıllık partition'lar
CREATE TABLE sales.orders_2024 PARTITION OF sales.orders_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE sales.orders_2025 PARTITION OF sales.orders_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

-- Partition'a otomatik yönlendirme
INSERT INTO sales.orders_partitioned (customer_id, total_amount, created_at)
VALUES (1, 5000, '2024-06-15'); -- sales.orders_2024'e gider

-- ----------------------------------------------------------------
-- Materialized View — Pahalı sorguyu önbellekle
-- ----------------------------------------------------------------
CREATE MATERIALIZED VIEW sales.monthly_revenue AS
SELECT
    DATE_TRUNC('month', created_at) AS month,
    COUNT(*)                         AS order_count,
    SUM(total_amount)                AS total_revenue,
    AVG(total_amount)                AS avg_order_value
FROM sales.orders
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month;

CREATE UNIQUE INDEX ON sales.monthly_revenue(month);

-- Zamanlanmış yenileme (cron job ile)
REFRESH MATERIALIZED VIEW CONCURRENTLY sales.monthly_revenue;

SELECT * FROM sales.monthly_revenue;
