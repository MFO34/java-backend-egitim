-- ============================================================
-- 03 — PACKAGES — PL/SQL'in temel organizasyon birimi
-- Specification (header) + Body (implementasyon)
-- ============================================================

-- ----------------------------------------------------------------
-- Package Specification — public API
-- ----------------------------------------------------------------
CREATE OR REPLACE PACKAGE hr_pkg AS

    -- Sabitler
    c_min_salary CONSTANT NUMBER := 20000;
    c_max_salary CONSTANT NUMBER := 200000;

    -- Custom exception
    e_invalid_salary EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_invalid_salary, -20100);

    -- Type tanımları
    TYPE t_emp_rec IS RECORD (
        emp_id   NUMBER,
        emp_name VARCHAR2(100),
        salary   NUMBER
    );

    TYPE t_emp_table IS TABLE OF t_emp_rec INDEX BY PLS_INTEGER;

    -- Public function ve procedure bildirimleri
    FUNCTION get_employee(p_id NUMBER) RETURN t_emp_rec;
    PROCEDURE hire_employee(
        p_name    IN VARCHAR2,
        p_dept_id IN NUMBER,
        p_salary  IN NUMBER,
        p_emp_id  OUT NUMBER
    );
    PROCEDURE fire_employee(p_id NUMBER);
    FUNCTION get_department_headcount(p_dept_id NUMBER) RETURN NUMBER;

END hr_pkg;
/

-- ----------------------------------------------------------------
-- Package Body — implementasyon
-- ----------------------------------------------------------------
CREATE OR REPLACE PACKAGE BODY hr_pkg AS

    -- Private variable (sadece bu package içinde görünür)
    g_call_count NUMBER := 0;

    -- Private helper function
    FUNCTION validate_salary(p_salary NUMBER) RETURN BOOLEAN IS
    BEGIN
        RETURN p_salary BETWEEN c_min_salary AND c_max_salary;
    END validate_salary;

    -- Public implementations
    FUNCTION get_employee(p_id NUMBER) RETURN t_emp_rec IS
        v_emp t_emp_rec;
    BEGIN
        g_call_count := g_call_count + 1;
        SELECT employee_id, last_name, salary
        INTO v_emp.emp_id, v_emp.emp_name, v_emp.salary
        FROM employees
        WHERE employee_id = p_id;
        RETURN v_emp;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20200, 'Çalışan bulunamadı: ' || p_id);
    END get_employee;

    PROCEDURE hire_employee(
        p_name    IN VARCHAR2,
        p_dept_id IN NUMBER,
        p_salary  IN NUMBER,
        p_emp_id  OUT NUMBER
    ) IS
    BEGIN
        IF NOT validate_salary(p_salary) THEN
            RAISE e_invalid_salary;
        END IF;

        SELECT employee_seq.NEXTVAL INTO p_emp_id FROM DUAL;

        INSERT INTO employees (employee_id, last_name, email, hire_date, job_id, salary, department_id)
        VALUES (p_emp_id, p_name, LOWER(p_name)||'@co.com', SYSDATE, 'IT_PROG', p_salary, p_dept_id);

        DBMS_OUTPUT.PUT_LINE('İşe alındı: ' || p_name || ' (ID=' || p_emp_id || ')');
    END hire_employee;

    PROCEDURE fire_employee(p_id NUMBER) IS
    BEGIN
        DELETE FROM employees WHERE employee_id = p_id;
        IF SQL%ROWCOUNT = 0 THEN
            RAISE_APPLICATION_ERROR(-20201, 'Silinecek çalışan bulunamadı: ' || p_id);
        END IF;
        DBMS_OUTPUT.PUT_LINE('Çalışan çıkarıldı: ' || p_id);
    END fire_employee;

    FUNCTION get_department_headcount(p_dept_id NUMBER) RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_count FROM employees WHERE department_id = p_dept_id;
        RETURN v_count;
    END get_department_headcount;

END hr_pkg;
/

-- ----------------------------------------------------------------
-- Package Kullanımı
-- ----------------------------------------------------------------
DECLARE
    v_emp    hr_pkg.t_emp_rec;
    v_new_id NUMBER;
BEGIN
    -- get_employee
    v_emp := hr_pkg.get_employee(100);
    DBMS_OUTPUT.PUT_LINE(v_emp.emp_name || ': ' || v_emp.salary);

    -- hire_employee
    hr_pkg.hire_employee('Ahmet Test', 10, 65000, v_new_id);
    DBMS_OUTPUT.PUT_LINE('Yeni ID: ' || v_new_id);

    -- headcount
    DBMS_OUTPUT.PUT_LINE('IT dept count: ' || hr_pkg.get_department_headcount(60));
END;
/
