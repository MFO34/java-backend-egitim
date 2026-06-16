-- ============================================================
-- 02 — PL/SQL TEMELLERİ
-- ============================================================

-- ----------------------------------------------------------------
-- 1. Anonymous Block
-- ----------------------------------------------------------------
DECLARE
    v_name      VARCHAR2(100);
    v_salary    NUMBER(10,2);
    v_bonus     NUMBER(10,2);
    c_rate      CONSTANT NUMBER := 0.10; -- sabit
BEGIN
    -- %TYPE — sütun tipini otomatik al
    SELECT last_name, salary
    INTO v_name, v_salary
    FROM employees
    WHERE employee_id = 100;

    v_bonus := v_salary * c_rate;

    DBMS_OUTPUT.PUT_LINE('Çalışan: ' || v_name);
    DBMS_OUTPUT.PUT_LINE('Maaş: ' || TO_CHAR(v_salary, '999,999.99'));
    DBMS_OUTPUT.PUT_LINE('Prim: ' || v_bonus);

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Çalışan bulunamadı');
    WHEN TOO_MANY_ROWS THEN
        DBMS_OUTPUT.PUT_LINE('Birden fazla satır döndü');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Hata: ' || SQLERRM);
END;
/

-- ----------------------------------------------------------------
-- 2. %ROWTYPE — Tüm satır tipi
-- ----------------------------------------------------------------
DECLARE
    v_emp employees%ROWTYPE;
BEGIN
    SELECT * INTO v_emp FROM employees WHERE employee_id = 100;
    DBMS_OUTPUT.PUT_LINE(v_emp.last_name || ' - ' || v_emp.salary);
END;
/

-- ----------------------------------------------------------------
-- 3. Control Flow
-- ----------------------------------------------------------------
DECLARE
    v_salary NUMBER := 75000;
    v_grade  VARCHAR2(20);
BEGIN
    -- IF-ELSIF-ELSE
    IF    v_salary > 90000 THEN v_grade := 'A';
    ELSIF v_salary > 70000 THEN v_grade := 'B';
    ELSIF v_salary > 50000 THEN v_grade := 'C';
    ELSE                        v_grade := 'D';
    END IF;

    DBMS_OUTPUT.PUT_LINE('Grade: ' || v_grade);

    -- CASE
    v_grade := CASE
        WHEN v_salary > 90000 THEN 'Senior'
        WHEN v_salary > 70000 THEN 'Mid'
        ELSE 'Junior'
    END;

    -- FOR LOOP
    FOR i IN 1..5 LOOP
        DBMS_OUTPUT.PUT_LINE('i = ' || i);
    END LOOP;

    -- WHILE LOOP
    DECLARE counter NUMBER := 0;
    BEGIN
        WHILE counter < 3 LOOP
            counter := counter + 1;
            DBMS_OUTPUT.PUT_LINE('Counter: ' || counter);
        END LOOP;
    END;
END;
/

-- ----------------------------------------------------------------
-- 4. Stored Function
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION get_annual_salary(
    p_employee_id IN NUMBER
) RETURN NUMBER
IS
    v_monthly NUMBER;
BEGIN
    SELECT salary INTO v_monthly
    FROM employees
    WHERE employee_id = p_employee_id;

    RETURN v_monthly * 12;
EXCEPTION
    WHEN NO_DATA_FOUND THEN RETURN 0;
END get_annual_salary;
/

-- Kullanım
SELECT get_annual_salary(100) FROM DUAL;
SELECT last_name, get_annual_salary(employee_id) AS annual_salary FROM employees;

-- ----------------------------------------------------------------
-- 5. Stored Procedure
-- ----------------------------------------------------------------
CREATE OR REPLACE PROCEDURE update_employee_salary(
    p_employee_id IN  NUMBER,
    p_new_salary  IN  NUMBER,
    p_old_salary  OUT NUMBER,
    p_success     OUT BOOLEAN
)
IS
BEGIN
    SELECT salary INTO p_old_salary
    FROM employees
    WHERE employee_id = p_employee_id;

    IF p_new_salary <= 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Maaş negatif olamaz');
    END IF;

    UPDATE employees
    SET salary = p_new_salary
    WHERE employee_id = p_employee_id;

    COMMIT;
    p_success := TRUE;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_success := FALSE;
        DBMS_OUTPUT.PUT_LINE('Hata: ' || SQLERRM);
END update_employee_salary;
/

-- Kullanım
DECLARE
    v_old_sal NUMBER;
    v_success BOOLEAN;
BEGIN
    update_employee_salary(100, 85000, v_old_sal, v_success);
    IF v_success THEN
        DBMS_OUTPUT.PUT_LINE('Güncellendi. Eski maaş: ' || v_old_sal);
    END IF;
END;
/
