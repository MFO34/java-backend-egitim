-- ============================================================
-- 05 — TRIGGERS — Otomatik tetiklenen işlemler
-- ============================================================

-- ----------------------------------------------------------------
-- 1. Audit Trigger — Her değişikliği kaydet
-- ----------------------------------------------------------------
CREATE TABLE hr.audit_log (
    id          SERIAL PRIMARY KEY,
    table_name  VARCHAR(100),
    operation   VARCHAR(10),
    old_data    JSONB,
    new_data    JSONB,
    changed_by  VARCHAR(100) DEFAULT CURRENT_USER,
    changed_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION hr.audit_trigger_fn()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO hr.audit_log (table_name, operation, old_data, new_data)
    VALUES (
        TG_TABLE_NAME,
        TG_OP,
        CASE WHEN TG_OP = 'DELETE' THEN row_to_json(OLD)::JSONB ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN row_to_json(NEW)::JSONB ELSE NULL END
    );

    RETURN CASE TG_OP WHEN 'DELETE' THEN OLD ELSE NEW END;
END;
$$;

CREATE TRIGGER trg_employees_audit
AFTER INSERT OR UPDATE OR DELETE ON hr.employees
FOR EACH ROW EXECUTE FUNCTION hr.audit_trigger_fn();

-- Test
UPDATE hr.employees SET salary = 90000 WHERE id = 1;
SELECT * FROM hr.audit_log;

-- ----------------------------------------------------------------
-- 2. Validation Trigger — İş kuralı kontrolü
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION hr.validate_employee_fn()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Maaş negatif olamaz
    IF NEW.salary <= 0 THEN
        RAISE EXCEPTION 'Maaş sıfır veya negatif olamaz: %', NEW.salary;
    END IF;

    -- Email formatı
    IF NEW.email NOT LIKE '%@%.%' THEN
        RAISE EXCEPTION 'Geçersiz email formatı: %', NEW.email;
    END IF;

    -- İşe giriş tarihi gelecek olamaz
    IF NEW.hire_date > CURRENT_DATE THEN
        RAISE EXCEPTION 'İşe giriş tarihi gelecekte olamaz: %', NEW.hire_date;
    END IF;

    -- updated_at güncelle (BEFORE trigger)
    -- NEW.updated_at := NOW();

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_employees_validate
BEFORE INSERT OR UPDATE ON hr.employees
FOR EACH ROW EXECUTE FUNCTION hr.validate_employee_fn();

-- ----------------------------------------------------------------
-- 3. Stock Trigger — Stok azaldığında uyarı
-- ----------------------------------------------------------------
CREATE TABLE sales.stock_alerts (
    id         SERIAL PRIMARY KEY,
    product_id INTEGER REFERENCES sales.products(id),
    old_stock  INTEGER,
    new_stock  INTEGER,
    alert_msg  TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION sales.check_stock_fn()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Stok 10'un altına düştüğünde uyar
    IF NEW.stock < 10 AND (OLD.stock IS NULL OR OLD.stock >= 10) THEN
        INSERT INTO sales.stock_alerts (product_id, old_stock, new_stock, alert_msg)
        VALUES (NEW.id, OLD.stock, NEW.stock,
                format('Kritik stok seviyesi: %s → %s adet', OLD.stock, NEW.stock));

        RAISE NOTICE '⚠ Stok uyarısı: % - % adet kaldı', NEW.name, NEW.stock;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_stock_check
AFTER UPDATE OF stock ON sales.products
FOR EACH ROW EXECUTE FUNCTION sales.check_stock_fn();

-- ----------------------------------------------------------------
-- 4. INSTEAD OF Trigger — View üzerinde DML
-- ----------------------------------------------------------------
CREATE VIEW hr.employee_summary AS
SELECT
    e.id, e.name, e.email, e.salary,
    d.name AS department_name
FROM hr.employees e
JOIN hr.departments d ON d.id = e.department_id;

CREATE OR REPLACE FUNCTION hr.employee_summary_update_fn()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE hr.employees
    SET name   = NEW.name,
        salary = NEW.salary,
        email  = NEW.email
    WHERE id = OLD.id;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_employee_summary_update
INSTEAD OF UPDATE ON hr.employee_summary
FOR EACH ROW EXECUTE FUNCTION hr.employee_summary_update_fn();

-- Test: View üzerinden güncelleme
UPDATE hr.employee_summary SET salary = 95000 WHERE id = 1;
