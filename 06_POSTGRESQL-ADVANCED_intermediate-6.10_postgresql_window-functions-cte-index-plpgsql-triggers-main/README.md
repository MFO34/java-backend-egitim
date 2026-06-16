# 06 — PostgreSQL Advanced
**Zorluk:** Intermediate (6/10) | **PostgreSQL 16** | **Mülakat Hazırlığı**

Window Functions, CTE, Index stratejileri, PL/pgSQL, Trigger, Transaction yönetimi, JSONB ve performans optimizasyonu.

---

## İçerik

| Konu | Dosya | Kapsanan Konular |
|------|-------|-----------------|
| **Setup** | `sql/00_setup.sql` | hr + sales şeması, örnek veriler |
| **Window Functions** | `sql/01_window_functions/` | ROW_NUMBER, RANK, DENSE_RANK, LAG, LEAD, FIRST_VALUE, Kümülatif SUM |
| **CTE** | `sql/02_cte/` | Basit CTE, Recursive CTE (org chart), Materialized CTE |
| **Indexes** | `sql/03_indexes/` | B-Tree, GIN, Partial, Composite, Covering Index, EXPLAIN ANALYZE |
| **PL/pgSQL** | `sql/04_plpgsql/` | Function, Procedure, Cursor, Exception handling |
| **Triggers** | `sql/05_triggers/` | Audit log, Validation, Stock alert, INSTEAD OF |
| **Transactions** | `sql/06_transactions/` | Isolation levels, Deadlock, Savepoint, FOR UPDATE |
| **JSONB** | `sql/07_jsonb/` | Operatörler, GIN index, Array işlemi, Aggregation |
| **Performance** | `sql/08_performance/` | EXPLAIN, pg_stat_statements, Partitioning, Materialized View |

---

## Kurulum

```bash
# PostgreSQL başlat
docker run -d --name pg-advanced \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=training \
  -p 5432:5432 postgres:16

# Bağlan ve çalıştır
psql -h localhost -U postgres -d training -f sql/00_setup.sql
psql -h localhost -U postgres -d training -f sql/01_window_functions/window_functions.sql
```

---

## Mülakat Soruları

**Q: ROW_NUMBER, RANK, DENSE_RANK farkı nedir?**
A: Tüm üçü sıralama fonksiyonları. Fark: aynı değer (tie) durumunda. `ROW_NUMBER`: her satıra benzersiz sıra (1,2,3,4). `RANK`: tie durumunda aynı sıra, sonraki atlanır (1,2,2,4). `DENSE_RANK`: tie durumunda aynı sıra, boşluk yok (1,2,2,3). Örnek: departman başına ilk 3 maaş → `ROW_NUMBER` veya `DENSE_RANK` kullan. `RANK` boşluk bırakır.

**Q: LAG/LEAD ne zaman kullanılır?**
A: Aynı satırdan önceki/sonraki satırın değerine erişmek için. Ay bazlı satış büyümesi: `LAG(satis, 1) OVER (ORDER BY ay)` ile önceki ay satışı. `(satis - LAG(satis,1)) / LAG(satis,1) * 100` büyüme oranı. `LEAD`: bir sonraki satırı önceden görmek. Örnek: "sonraki toplantı tarihi". Self-join alternatifindan çok daha verimli — tek pass.

**Q: Her departmandan en yüksek maaşlı kişiyi nasıl bulursun?**
A: Window function ile: `WHERE rn = 1` — `ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC)`. Alternatif: Correlated subquery — `WHERE salary = (SELECT MAX(salary) FROM employees e2 WHERE e2.dept_id = e1.dept_id)`. Tie durumunda fark: `ROW_NUMBER` bir kişi seçer, `RANK` veya subquery tüm tie'ları getirir. Join ile: `INNER JOIN (SELECT dept_id, MAX(salary) max_sal FROM ...) m ON ...`.

**Q: B-Tree ve GIN index farkı nedir?**
A: B-Tree: Sıralı veri için. `=`, `<`, `>`, `BETWEEN`, `LIKE 'prefix%'` operatörleri. Çoğu kolonda default index. GIN (Generalized Inverted Index): Bileşik değerler için — `JSONB`, `tsvector` (full-text), `ARRAY`. `@>`, `&&`, `@@` operatörleri. Örnek: JSONB alanında `WHERE data @> '{"status": "active"}'` → GIN gerekli. B-Tree JSONB içini tarayamaz. GiST: geometrik veri için.

**Q: Partial index ne zaman tercih edilir?**
A: Tablo büyük ama sorgu küçük bir subset üzerinde. `CREATE INDEX idx_active ON products(name) WHERE is_active = TRUE`. Sadece aktif ürünleri sorguluyorsak tüm tablo index'i yerine aktif ürünler index'i → daha küçük, daha hızlı. `WHERE deleted_at IS NULL` — soft delete'de silinmemişler için. Index boyutu küçülür, write overhead azalır. Trade-off: her sorgu partial index'in WHERE koşuluyla eşleşmelidir.

**Q: ACID nedir?**
A: Atomicity (Atomiklik): Transaction ya tamamen başarılı ya hiç. Havale: hem borç hem alacak ya ikisi de yazılır ya hiçbiri. Consistency (Tutarlılık): Her transaction DB'yi geçerli durumdan geçerli duruma götürür. Constraint'ler korunur. Isolation (İzolasyon): Eş zamanlı transaction'lar birbirini görmez (isolation level'a göre). Durability (Kalıcılık): Commit edilen veri sistem çöküşünde kaybolmaz (WAL log ile).

**Q: Dirty read, non-repeatable read, phantom read nedir?**
A: Dirty read: Commit edilmemiş veriyi okuma. T1 güncelledi ama rollback yapacak, T2 güncellemeyi okur — anlamsız veri. Non-repeatable read: Aynı satırı iki kez okuyunca farklı sonuç — T2 arada güncelledi. Phantom read: Aynı sorguyu iki kez çalıştırınca farklı satır sayısı — T2 araya satır ekledi/sildi. Çözüm: Isolation seviyesi artırılır. PostgreSQL: READ COMMITTED (default), REPEATABLE READ, SERIALIZABLE.

**Q: PostgreSQL MVCC nasıl çalışır?**
A: Multi-Version Concurrency Control: Her satır birden fazla versiyon tutulabilir. Transaction başlayınca snapshot alır — kendi başlangıcından önce commit edilmiş verileri görür. Yazma işlemleri eski satırı silmez, yeni versiyon ekler (xmax/xmin transaction ID). Read hiçbir lock almaz — yazma işlemini beklemez. Dead tuple'lar (eski versiyonlar) VACUUM ile temizlenir. Avantaj: read-write çakışması yok, yüksek concurrency.

**Q: SELECT FOR UPDATE ne zaman kullanılır?**
A: Okunup değiştirilecek satırda başka transaction değişiklik yapamasın. Örnek: stok rezervasyonu — `SELECT stock FROM products WHERE id=1 FOR UPDATE` → stok=10, sonra `UPDATE products SET stock=9`. Arada başka transaction da okuyup güncellemeye çalışırsa bekler (pessimistic locking). Alternatif: Optimistic locking (@Version) — lock almaz, commit sırasında conflict tespiti. `FOR UPDATE` kritik bölgeler için, `@Version` düşük conflict senaryoları için.

**Q: PL/pgSQL Function ile Procedure farkı nedir?**
A: Function: Değer döndürür (`RETURNS`), SELECT içinde kullanılabilir, kendi içinde transaction başlatamaz. `CREATE FUNCTION ...`. Procedure: Değer döndürmeyebilir (`CALL` ile çağrılır), PostgreSQL 11+: `COMMIT`/`ROLLBACK` yapabilir — birden fazla transaction yönetebilir. Büyük batch işlemlerde ara commit: `FOR i IN 1..1000000 LOOP ... IF MOD(i,1000)=0 THEN COMMIT; END IF; END LOOP;`

---

**Önceki →** [05 - Student Tracker JDBC](../05_STUDENT-TRACKER)
**Sonraki →** [07 - Oracle PL/SQL](../07_ORACLE-PLSQL)
