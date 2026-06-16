-- ============================================================
-- 06 — TRANSACTIONS & ISOLATION LEVELS
-- ACID: Atomicity, Consistency, Isolation, Durability
-- ============================================================

-- ----------------------------------------------------------------
-- Isolation Levels (Düşükten yükseğe)
--
-- READ UNCOMMITTED → dirty read var (PostgreSQL'de READ COMMITTED gibi davranır)
-- READ COMMITTED   → dirty read yok, non-repeatable read var (varsayılan)
-- REPEATABLE READ  → dirty + non-repeatable read yok, phantom read var
-- SERIALIZABLE     → tüm sorunlar yok, en yüksek izolasyon
-- ----------------------------------------------------------------

-- READ COMMITTED (PostgreSQL varsayılanı)
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT salary FROM hr.employees WHERE id = 1;
-- Başka bir session aynı anda UPDATE yapabilir
-- ikinci SELECT farklı sonuç döndürebilir (non-repeatable read)
COMMIT;

-- REPEATABLE READ
BEGIN;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT salary FROM hr.employees WHERE id = 1; -- X değeri
-- Başka session UPDATE yapsa da
SELECT salary FROM hr.employees WHERE id = 1; -- yine X değeri (snapshot)
COMMIT;

-- SERIALIZABLE
BEGIN;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
-- Tüm transaction'lar sıralı çalışıyormuş gibi davranır
-- En güvenli, en yavaş
COMMIT;

-- ----------------------------------------------------------------
-- Deadlock Örneği ve Önleme
-- ----------------------------------------------------------------
-- Session 1:
BEGIN;
UPDATE hr.employees SET salary = salary + 1000 WHERE id = 1; -- lock on id=1
UPDATE hr.employees SET salary = salary + 1000 WHERE id = 2; -- lock on id=2 (wait)
COMMIT;

-- Session 2 (eş zamanlı):
BEGIN;
UPDATE hr.employees SET salary = salary + 500 WHERE id = 2; -- lock on id=2
UPDATE hr.employees SET salary = salary + 500 WHERE id = 1; -- deadlock!
COMMIT;

-- ÇÖZÜM: Her zaman aynı sırayla lock al
-- Her iki session da önce id=1 sonra id=2'yi güncellemeli

-- ----------------------------------------------------------------
-- Savepoint — Kısmi rollback
-- ----------------------------------------------------------------
BEGIN;
    INSERT INTO hr.employees (name, email, department_id, salary, hire_date)
    VALUES ('Test User', 'test@co.com', 1, 50000, CURRENT_DATE);

    SAVEPOINT sp1; -- checkpoint

    UPDATE hr.employees SET salary = -100 WHERE email = 'test@co.com'; -- hatalı

    ROLLBACK TO SAVEPOINT sp1; -- yalnızca UPDATE'i geri al

    UPDATE hr.employees SET salary = 55000 WHERE email = 'test@co.com'; -- doğru

COMMIT;

-- ----------------------------------------------------------------
-- SELECT FOR UPDATE — Pessimistic Lock
-- ----------------------------------------------------------------
BEGIN;
SELECT stock FROM sales.products WHERE id = 1 FOR UPDATE; -- diğerleri bekler
-- stoku kontrol et ve güncelle
UPDATE sales.products SET stock = stock - 1 WHERE id = 1;
COMMIT;

-- SELECT FOR UPDATE SKIP LOCKED — queue işleme
BEGIN;
SELECT id, customer_id FROM sales.orders
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 10
FOR UPDATE SKIP LOCKED; -- başkaları tarafından kilitli olanları atla
-- Bu teknik job queue implementasyonunda kullanılır
COMMIT;

-- ----------------------------------------------------------------
-- Advisory Lock — Uygulama seviyesi kilit
-- ----------------------------------------------------------------
-- Distributed lock için (çok instance uygulamalarda)
SELECT pg_try_advisory_lock(12345);  -- kilit al (boolean döner)
-- kritik bölüm
SELECT pg_advisory_unlock(12345);   -- kilit bırak

-- ----------------------------------------------------------------
-- Vacuum & Analyze (MVCC temizliği)
-- ----------------------------------------------------------------
VACUUM ANALYZE hr.employees;
-- MVCC: PostgreSQL her UPDATE'de yeni satır yazar, eskisini işaretler
-- VACUUM eski sürümleri temizler
