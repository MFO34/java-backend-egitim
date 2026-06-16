-- ============================================================
-- 01 — WINDOW FUNCTIONS
-- Syntax: function() OVER (PARTITION BY ... ORDER BY ...)
-- Aggregate'ten farkı: satırları gruplamaz, hepsini gösterir
-- ============================================================

-- ----------------------------------------------------------------
-- ROW_NUMBER — Gruplarda benzersiz sıra numarası
-- Mülakat: "Her departmanın en yüksek maaşlı çalışanını bul"
-- ----------------------------------------------------------------
SELECT
    name,
    department_id,
    salary,
    ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) AS rn
FROM hr.employees;

-- Her departmandan en yüksek maaşlı 1 kişi (CTE ile)
WITH ranked AS (
    SELECT
        e.name,
        d.name AS department,
        e.salary,
        ROW_NUMBER() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) AS rn
    FROM hr.employees e
    JOIN hr.departments d ON d.id = e.department_id
)
SELECT name, department, salary
FROM ranked
WHERE rn = 1;

-- ----------------------------------------------------------------
-- RANK vs DENSE_RANK vs ROW_NUMBER
-- RANK       → aynı değere aynı sıra, sonrakinde atlama (1,1,3)
-- DENSE_RANK → aynı değere aynı sıra, atlama yok (1,1,2)
-- ROW_NUMBER → her satıra benzersiz sıra (1,2,3)
-- ----------------------------------------------------------------
SELECT
    name,
    salary,
    ROW_NUMBER()  OVER (ORDER BY salary DESC) AS row_num,
    RANK()        OVER (ORDER BY salary DESC) AS rnk,
    DENSE_RANK()  OVER (ORDER BY salary DESC) AS dense_rnk,
    NTILE(3)      OVER (ORDER BY salary DESC) AS salary_tier  -- 3 gruba böl
FROM hr.employees;

-- ----------------------------------------------------------------
-- LAG & LEAD — Önceki/sonraki satır değeri
-- Mülakat: "Her çalışanın önceki ay satışına göre değişimi"
-- ----------------------------------------------------------------
SELECT
    name,
    salary,
    LAG(salary)  OVER (PARTITION BY department_id ORDER BY hire_date) AS prev_hire_salary,
    LEAD(salary) OVER (PARTITION BY department_id ORDER BY hire_date) AS next_hire_salary,
    salary - LAG(salary, 1, salary) OVER (PARTITION BY department_id ORDER BY hire_date) AS salary_diff
FROM hr.employees;

-- ----------------------------------------------------------------
-- FIRST_VALUE & LAST_VALUE — Penceredeki ilk/son değer
-- ----------------------------------------------------------------
SELECT
    name,
    department_id,
    salary,
    FIRST_VALUE(salary) OVER (PARTITION BY department_id ORDER BY salary DESC
                               ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS dept_max_salary,
    LAST_VALUE(salary)  OVER (PARTITION BY department_id ORDER BY salary DESC
                               ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS dept_min_salary
FROM hr.employees;

-- ----------------------------------------------------------------
-- SUM / AVG kümülatif (Running Total)
-- Mülakat: "Kümülatif satış toplamını hesapla"
-- ----------------------------------------------------------------
SELECT
    o.id,
    o.created_at::DATE AS order_date,
    o.total_amount,
    SUM(o.total_amount) OVER (ORDER BY o.created_at)                        AS running_total,
    AVG(o.total_amount) OVER (ORDER BY o.created_at ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS moving_avg_3
FROM sales.orders o
ORDER BY o.created_at;

-- ----------------------------------------------------------------
-- PERCENT_RANK & CUME_DIST — Yüzdelik sıralama
-- ----------------------------------------------------------------
SELECT
    name,
    salary,
    ROUND(PERCENT_RANK() OVER (ORDER BY salary)::NUMERIC * 100, 2) AS percentile,
    ROUND(CUME_DIST()    OVER (ORDER BY salary)::NUMERIC * 100, 2) AS cumulative_pct
FROM hr.employees;
