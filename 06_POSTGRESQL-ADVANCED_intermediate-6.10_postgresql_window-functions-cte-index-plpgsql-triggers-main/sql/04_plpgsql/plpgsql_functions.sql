-- ============================================================
-- 04 — PL/pgSQL — Stored Procedure ve Function
-- ============================================================

-- ----------------------------------------------------------------
-- 1. Basit Function — Maaş hesaplama
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION hr.calculate_bonus(
    p_employee_id INTEGER,
    p_bonus_rate  NUMERIC DEFAULT 0.10
)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
    v_salary  NUMERIC;
    v_bonus   NUMERIC;
BEGIN
    SELECT salary INTO v_salary
    FROM hr.employees
    WHERE id = p_employee_id AND is_active = TRUE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Çalışan bulunamadı: %', p_employee_id;
    END IF;

    v_bonus := v_salary * p_bonus_rate;
    RETURN v_bonus;

EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Hata: % - %', SQLSTATE, SQLERRM;
        RETURN 0;
END;
$$;

-- Kullanım
SELECT hr.calculate_bonus(1);
SELECT hr.calculate_bonus(1, 0.15);

-- ----------------------------------------------------------------
-- 2. Procedure — Transaction yönetimli işlem
-- ----------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sales.process_order(
    p_customer_id INTEGER,
    p_items       JSONB  -- [{"product_id": 1, "quantity": 2}, ...]
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_order_id    INTEGER;
    v_total       NUMERIC := 0;
    v_item        JSONB;
    v_product_id  INTEGER;
    v_quantity    INTEGER;
    v_price       NUMERIC;
    v_stock       INTEGER;
BEGIN
    -- Sipariş oluştur
    INSERT INTO sales.orders (customer_id, total_amount, status)
    VALUES (p_customer_id, 0, 'PENDING')
    RETURNING id INTO v_order_id;

    -- Her ürünü işle
    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        v_product_id := (v_item->>'product_id')::INTEGER;
        v_quantity   := (v_item->>'quantity')::INTEGER;

        -- Stok ve fiyat kontrolü
        SELECT price, stock INTO v_price, v_stock
        FROM sales.products
        WHERE id = v_product_id
        FOR UPDATE; -- pessimistic lock

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Ürün bulunamadı: %', v_product_id;
        END IF;

        IF v_stock < v_quantity THEN
            RAISE EXCEPTION 'Yetersiz stok: ürün=%, mevcut=%, istenen=%',
                v_product_id, v_stock, v_quantity;
        END IF;

        -- Sipariş kalemi ekle
        INSERT INTO sales.order_items (order_id, product_id, quantity, unit_price)
        VALUES (v_order_id, v_product_id, v_quantity, v_price);

        -- Stok düşür
        UPDATE sales.products
        SET stock = stock - v_quantity
        WHERE id = v_product_id;

        v_total := v_total + (v_price * v_quantity);
    END LOOP;

    -- Toplam güncelle
    UPDATE sales.orders SET total_amount = v_total WHERE id = v_order_id;

    RAISE NOTICE 'Sipariş oluşturuldu: id=%, toplam=%', v_order_id, v_total;

EXCEPTION
    WHEN OTHERS THEN
        -- Procedure'de ROLLBACK yapılabilir
        RAISE NOTICE 'Sipariş hatası: %', SQLERRM;
        RAISE;
END;
$$;

-- Kullanım
CALL sales.process_order(1, '[{"product_id": 1, "quantity": 1}, {"product_id": 4, "quantity": 2}]');

-- ----------------------------------------------------------------
-- 3. Function — Table döndürme
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION hr.get_department_summary()
RETURNS TABLE (
    department_name VARCHAR,
    employee_count  BIGINT,
    avg_salary      NUMERIC,
    min_salary      NUMERIC,
    max_salary      NUMERIC,
    total_payroll   NUMERIC
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        d.name::VARCHAR,
        COUNT(e.id),
        ROUND(AVG(e.salary), 2),
        MIN(e.salary),
        MAX(e.salary),
        SUM(e.salary)
    FROM hr.departments d
    LEFT JOIN hr.employees e ON e.department_id = d.id AND e.is_active = TRUE
    GROUP BY d.id, d.name
    ORDER BY total_payroll DESC;
END;
$$;

SELECT * FROM hr.get_department_summary();

-- ----------------------------------------------------------------
-- 4. Cursor kullanımı — Büyük veri setlerinde bellek yönetimi
-- ----------------------------------------------------------------
CREATE OR REPLACE PROCEDURE hr.bulk_salary_update(p_increase_pct NUMERIC)
LANGUAGE plpgsql
AS $$
DECLARE
    cur CURSOR FOR
        SELECT id, name, salary FROM hr.employees WHERE is_active = TRUE;
    rec hr.employees%ROWTYPE;
    v_count INTEGER := 0;
BEGIN
    OPEN cur;
    LOOP
        FETCH cur INTO rec;
        EXIT WHEN NOT FOUND;

        UPDATE hr.employees
        SET salary = salary * (1 + p_increase_pct / 100)
        WHERE id = rec.id;

        v_count := v_count + 1;
    END LOOP;
    CLOSE cur;

    RAISE NOTICE '% çalışanın maaşı %% artırıldı', v_count, p_increase_pct;
END;
$$;
