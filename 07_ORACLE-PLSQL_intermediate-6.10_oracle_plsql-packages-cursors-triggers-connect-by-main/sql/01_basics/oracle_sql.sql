-- ============================================================
-- 01 — ORACLE'A ÖZGÜ SQL
-- PostgreSQL'den farklar ve Oracle spesifik özellikler
-- ============================================================

-- ----------------------------------------------------------------
-- DUAL — Oracle'ın sistem tablosu (PostgreSQL'de gerek yok)
-- ----------------------------------------------------------------
SELECT SYSDATE FROM DUAL;
SELECT 1 + 1   FROM DUAL;
SELECT SYS_GUID() FROM DUAL; -- UUID üretimi

-- ----------------------------------------------------------------
-- ROWNUM vs ROW_NUMBER()
-- ROWNUM → WHERE koşulundan önce atanır (ilk N satır için)
-- ROW_NUMBER() → sıralama sonrası (doğru kullanım)
-- ----------------------------------------------------------------

-- YANLIŞ: İlk 5 yüksek maaşlı çalışan (sıralama sonrası ROWNUM çalışmaz)
-- SELECT * FROM employees WHERE ROWNUM <= 5 ORDER BY salary DESC;

-- DOĞRU: Subquery ile
SELECT * FROM (
    SELECT e.*, ROWNUM AS rn
    FROM (SELECT * FROM employees ORDER BY salary DESC) e
) WHERE rn <= 5;

-- DOĞRU: ROW_NUMBER() ile (Oracle 12c+)
SELECT * FROM employees
ORDER BY salary DESC
FETCH FIRST 5 ROWS ONLY; -- Oracle 12c+

-- ----------------------------------------------------------------
-- NVL, NVL2, NULLIF, COALESCE
-- ----------------------------------------------------------------
SELECT
    name,
    NVL(commission_pct, 0)                          AS commission,   -- NULL → 0
    NVL2(commission_pct, 'Komisyonlu', 'Sabit')     AS pay_type,    -- NULL? if/else
    NULLIF(department_id, 10)                        AS dept,         -- equal → NULL
    COALESCE(commission_pct, bonus_rate, 0)         AS any_extra    -- ilk non-null
FROM employees;

-- ----------------------------------------------------------------
-- DECODE — Oracle'ın CASE ifadesi
-- ----------------------------------------------------------------
SELECT
    name,
    salary,
    DECODE(department_id,
        10, 'Muhasebe',
        20, 'Araştırma',
        30, 'Satış',
        'Diğer') AS department_name
FROM employees;

-- ----------------------------------------------------------------
-- CONNECT BY — Hiyerarşik sorgular
-- Mülakat: "Oracle'da org chart nasıl çekilir?"
-- ----------------------------------------------------------------
SELECT
    LEVEL,
    LPAD(' ', (LEVEL-1)*3) || last_name AS org_chart,
    employee_id,
    manager_id,
    SYS_CONNECT_BY_PATH(last_name, '/') AS full_path
FROM employees
START WITH manager_id IS NULL          -- kökten başla
CONNECT BY PRIOR employee_id = manager_id  -- parent-child ilişkisi
ORDER SIBLINGS BY last_name;

-- CONNECT_BY_ROOT — Her satır için köküne ulaş
SELECT
    employee_id, last_name,
    CONNECT_BY_ROOT last_name AS ceo_name,
    LEVEL
FROM employees
START WITH manager_id IS NULL
CONNECT BY PRIOR employee_id = manager_id;

-- ----------------------------------------------------------------
-- PIVOT & UNPIVOT
-- ----------------------------------------------------------------
-- PIVOT: Satırları sütuna dönüştür
SELECT * FROM (
    SELECT department_id, job_id, salary FROM employees
)
PIVOT (
    SUM(salary)
    FOR job_id IN ('IT_PROG' AS IT, 'SA_REP' AS Sales, 'AD_VP' AS VP)
);

-- UNPIVOT: Sütunları satıra dönüştür
SELECT department, metric, value
FROM dept_summary
UNPIVOT (
    value FOR metric IN (avg_salary, min_salary, max_salary)
);

-- ----------------------------------------------------------------
-- MERGE — Upsert
-- ----------------------------------------------------------------
MERGE INTO target_employees t
USING (SELECT * FROM staging_employees) s
ON (t.employee_id = s.employee_id)
WHEN MATCHED THEN
    UPDATE SET t.salary = s.salary, t.department_id = s.department_id
WHEN NOT MATCHED THEN
    INSERT (employee_id, name, salary, department_id)
    VALUES (s.employee_id, s.name, s.salary, s.department_id);

-- ----------------------------------------------------------------
-- Analytic Functions (Window Functions)
-- ----------------------------------------------------------------
SELECT
    employee_id, last_name, department_id, salary,
    SUM(salary)    OVER (PARTITION BY department_id) AS dept_total,
    AVG(salary)    OVER (PARTITION BY department_id) AS dept_avg,
    RATIO_TO_REPORT(salary) OVER (PARTITION BY department_id) AS pct_of_dept,
    LAG(salary, 1) OVER (PARTITION BY department_id ORDER BY salary) AS prev_salary,
    LEAD(salary,1) OVER (PARTITION BY department_id ORDER BY salary) AS next_salary
FROM employees;

-- ----------------------------------------------------------------
-- FLASHBACK — Geçmiş veriye erişim
-- ----------------------------------------------------------------
SELECT * FROM employees AS OF TIMESTAMP (SYSTIMESTAMP - INTERVAL '1' HOUR);
SELECT * FROM employees AS OF SCN 12345678;

-- ----------------------------------------------------------------
-- Sequence
-- ----------------------------------------------------------------
CREATE SEQUENCE employee_seq
    START WITH 1000
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Kullanım
INSERT INTO employees (employee_id, name) VALUES (employee_seq.NEXTVAL, 'Test');
SELECT employee_seq.CURRVAL FROM DUAL;
