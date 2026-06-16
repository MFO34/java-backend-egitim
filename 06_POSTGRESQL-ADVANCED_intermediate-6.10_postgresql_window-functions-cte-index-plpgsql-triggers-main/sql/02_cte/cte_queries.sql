-- ============================================================
-- 02 — CTE (Common Table Expressions)
-- WITH clause — sorgu okunabilirliği ve yeniden kullanım
-- ============================================================

-- ----------------------------------------------------------------
-- Basit CTE — Okunabilirliği artırır
-- ----------------------------------------------------------------
WITH high_earners AS (
    SELECT id, name, salary, department_id
    FROM hr.employees
    WHERE salary > 70000
),
dept_stats AS (
    SELECT department_id, AVG(salary) AS avg_salary, COUNT(*) AS cnt
    FROM hr.employees
    GROUP BY department_id
)
SELECT
    e.name,
    e.salary,
    d.name AS department,
    ds.avg_salary AS dept_avg,
    ROUND(e.salary - ds.avg_salary, 2) AS above_avg
FROM high_earners e
JOIN hr.departments d  ON d.id = e.department_id
JOIN dept_stats ds     ON ds.department_id = e.department_id
ORDER BY e.salary DESC;

-- ----------------------------------------------------------------
-- Recursive CTE — Hiyerarşik yapılar (org chart, kategori ağacı)
-- Mülakat: "Yönetici hiyerarşisini listele"
-- ----------------------------------------------------------------
WITH RECURSIVE org_chart AS (
    -- Base case: en üst yöneticiler (manager_id NULL)
    SELECT
        id, name, manager_id, salary,
        1 AS level,
        name::TEXT AS path
    FROM hr.employees
    WHERE manager_id IS NULL

    UNION ALL

    -- Recursive case: bir alt seviye
    SELECT
        e.id, e.name, e.manager_id, e.salary,
        oc.level + 1,
        oc.path || ' → ' || e.name
    FROM hr.employees e
    JOIN org_chart oc ON oc.id = e.manager_id
)
SELECT
    REPEAT('  ', level - 1) || name AS hierarchy,
    level,
    salary,
    path
FROM org_chart
ORDER BY path;

-- ----------------------------------------------------------------
-- CTE ile UPDATE — Satış siparişlerinin durumunu güncelle
-- ----------------------------------------------------------------
WITH old_pending AS (
    SELECT id
    FROM sales.orders
    WHERE status = 'PENDING'
      AND created_at < NOW() - INTERVAL '30 days'
)
UPDATE sales.orders
SET status = 'CANCELLED'
WHERE id IN (SELECT id FROM old_pending);

-- ----------------------------------------------------------------
-- CTE ile DELETE — Orphan kayıtları temizle
-- ----------------------------------------------------------------
WITH orphan_items AS (
    SELECT oi.id
    FROM sales.order_items oi
    LEFT JOIN sales.orders o ON o.id = oi.order_id
    WHERE o.id IS NULL
)
DELETE FROM sales.order_items
WHERE id IN (SELECT id FROM orphan_items);

-- ----------------------------------------------------------------
-- Fibonacci (Recursive CTE — klasik mülakat sorusu)
-- ----------------------------------------------------------------
WITH RECURSIVE fibonacci(n, a, b) AS (
    SELECT 1, 0::BIGINT, 1::BIGINT
    UNION ALL
    SELECT n + 1, b, a + b
    FROM fibonacci
    WHERE n < 15
)
SELECT n, a AS fibonacci_number FROM fibonacci;

-- ----------------------------------------------------------------
-- Materialized CTE (PostgreSQL 12+)
-- WITH x AS MATERIALIZED → her zaman somutlaştır (cache)
-- WITH x AS NOT MATERIALIZED → planner optimizasyonuna bırak
-- ----------------------------------------------------------------
WITH RECURSIVE category_tree AS MATERIALIZED (
    SELECT
        p.id, p.name, p.category,
        p.price,
        AVG(p.price) OVER (PARTITION BY p.category) AS category_avg_price
    FROM sales.products p
)
SELECT * FROM category_tree WHERE price > category_avg_price;
