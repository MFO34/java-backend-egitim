-- ============================================================
-- 04 — CURSORS — Satır satır veri işleme
-- ============================================================

-- ----------------------------------------------------------------
-- 1. Implicit Cursor — SQL%ROWCOUNT, SQL%FOUND
-- ----------------------------------------------------------------
BEGIN
    UPDATE employees SET salary = salary * 1.10
    WHERE department_id = 60;

    IF SQL%FOUND THEN
        DBMS_OUTPUT.PUT_LINE(SQL%ROWCOUNT || ' satır güncellendi');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Hiç satır güncellenmedi');
    END IF;
END;
/

-- ----------------------------------------------------------------
-- 2. Explicit Cursor
-- ----------------------------------------------------------------
DECLARE
    CURSOR emp_cursor IS
        SELECT employee_id, last_name, salary, department_id
        FROM employees
        WHERE salary > 10000
        ORDER BY salary DESC;

    v_emp emp_cursor%ROWTYPE;
BEGIN
    OPEN emp_cursor;
    LOOP
        FETCH emp_cursor INTO v_emp;
        EXIT WHEN emp_cursor%NOTFOUND;

        DBMS_OUTPUT.PUT_LINE(
            v_emp.last_name || ' - ' || v_emp.salary ||
            ' (Satır: ' || emp_cursor%ROWCOUNT || ')'
        );
    END LOOP;
    CLOSE emp_cursor;
END;
/

-- ----------------------------------------------------------------
-- 3. Cursor FOR LOOP — En temiz kullanım (önerilen)
-- ----------------------------------------------------------------
DECLARE
    CURSOR dept_emp_cur IS
        SELECT d.department_name, e.last_name, e.salary
        FROM departments d
        JOIN employees e ON e.department_id = d.department_id
        ORDER BY d.department_name, e.salary DESC;
BEGIN
    FOR rec IN dept_emp_cur LOOP
        DBMS_OUTPUT.PUT_LINE(
            RPAD(rec.department_name, 20) ||
            RPAD(rec.last_name, 20) ||
            TO_CHAR(rec.salary, '99,999')
        );
    END LOOP;
END;
/

-- ----------------------------------------------------------------
-- 4. Parametreli Cursor
-- ----------------------------------------------------------------
DECLARE
    CURSOR emp_by_dept(p_dept_id NUMBER, p_min_salary NUMBER DEFAULT 0) IS
        SELECT last_name, salary FROM employees
        WHERE department_id = p_dept_id AND salary >= p_min_salary
        ORDER BY salary DESC;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== IT Departmanı ===');
    FOR rec IN emp_by_dept(60, 5000) LOOP
        DBMS_OUTPUT.PUT_LINE(rec.last_name || ': ' || rec.salary);
    END LOOP;

    DBMS_OUTPUT.PUT_LINE('=== Satış Departmanı ===');
    FOR rec IN emp_by_dept(80) LOOP
        DBMS_OUTPUT.PUT_LINE(rec.last_name || ': ' || rec.salary);
    END LOOP;
END;
/

-- ----------------------------------------------------------------
-- 5. REF CURSOR — Dinamik cursor
-- ----------------------------------------------------------------
CREATE OR REPLACE PROCEDURE get_employees_by_criteria(
    p_criteria IN VARCHAR2,  -- 'DEPT', 'SALARY', 'ALL'
    p_value    IN NUMBER,
    p_cursor   OUT SYS_REFCURSOR
)
IS
BEGIN
    CASE p_criteria
        WHEN 'DEPT' THEN
            OPEN p_cursor FOR
                SELECT employee_id, last_name, salary
                FROM employees WHERE department_id = p_value;
        WHEN 'SALARY' THEN
            OPEN p_cursor FOR
                SELECT employee_id, last_name, salary
                FROM employees WHERE salary > p_value;
        ELSE
            OPEN p_cursor FOR
                SELECT employee_id, last_name, salary FROM employees;
    END CASE;
END;
/

-- Kullanım
DECLARE
    v_cursor SYS_REFCURSOR;
    v_id     NUMBER;
    v_name   VARCHAR2(100);
    v_salary NUMBER;
BEGIN
    get_employees_by_criteria('DEPT', 60, v_cursor);
    LOOP
        FETCH v_cursor INTO v_id, v_name, v_salary;
        EXIT WHEN v_cursor%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(v_name || ': ' || v_salary);
    END LOOP;
    CLOSE v_cursor;
END;
/

-- ----------------------------------------------------------------
-- 6. BULK COLLECT & FORALL — Performanslı toplu işlem
-- ----------------------------------------------------------------
DECLARE
    TYPE t_emp_ids   IS TABLE OF NUMBER;
    TYPE t_salaries  IS TABLE OF NUMBER;

    v_emp_ids  t_emp_ids;
    v_salaries t_salaries;
BEGIN
    -- BULK COLLECT: Tüm sonucu tek seferde belleğe al
    SELECT employee_id, salary
    BULK COLLECT INTO v_emp_ids, v_salaries
    FROM employees WHERE department_id = 60;

    DBMS_OUTPUT.PUT_LINE(v_emp_ids.COUNT || ' çalışan yüklendi');

    -- FORALL: Toplu DML (tek tek yapılan UPDATE'ten çok daha hızlı)
    FORALL i IN v_emp_ids.FIRST..v_emp_ids.LAST
        UPDATE employees
        SET salary = v_salaries(i) * 1.05
        WHERE employee_id = v_emp_ids(i);

    DBMS_OUTPUT.PUT_LINE(SQL%ROWCOUNT || ' satır güncellendi');
    COMMIT;
END;
/
