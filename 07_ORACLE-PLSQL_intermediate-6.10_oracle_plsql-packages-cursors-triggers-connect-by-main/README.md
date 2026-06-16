# 07 — Oracle PL/SQL
**Zorluk:** Intermediate (6/10) | **Oracle 19c+** | **Mülakat Hazırlığı**

Oracle'a özgü SQL, PL/SQL, Package, Cursor, Trigger ve ileri seviye özellikler.

---

## İçerik

| Konu | Dosya | Kapsanan Konular |
|------|-------|-----------------|
| **Oracle SQL** | `sql/01_basics/oracle_sql.sql` | DUAL, ROWNUM, NVL/DECODE, CONNECT BY, PIVOT, MERGE, Sequence |
| **PL/SQL Temelleri** | `sql/02_plsql/plsql_basics.sql` | Anonymous block, %TYPE/%ROWTYPE, IF/LOOP, Function, Procedure |
| **Packages** | `sql/03_packages/packages.sql` | Specification, Body, private/public, custom exception, PRAGMA |
| **Cursors** | `sql/04_cursors/cursors.sql` | Implicit, Explicit, FOR LOOP, Parametreli, REF CURSOR, BULK COLLECT, FORALL |

---

## PostgreSQL vs Oracle Farkları

```
Özellik              | PostgreSQL          | Oracle
---------------------+---------------------+----------------------
Sayfalama            | LIMIT / OFFSET       | ROWNUM / FETCH FIRST
Null kontrolü        | COALESCE            | NVL / NVL2
Hiyerarşi           | Recursive CTE       | CONNECT BY
Upsert              | INSERT ON CONFLICT   | MERGE
Prosedür dili       | PL/pgSQL            | PL/SQL
Modülerizasyon      | Schema + Function   | Package
Veri tipi           | VARCHAR             | VARCHAR2
String birleştirme  | ||                  | || veya CONCAT
Tarih               | NOW()               | SYSDATE / SYSTIMESTAMP
Auto increment      | SERIAL / IDENTITY   | SEQUENCE + TRIGGER
```

---

## Mülakat Soruları

**Q: ROWNUM ile ilk 10 satırı nasıl alırsın?**
A: `SELECT * FROM (SELECT * FROM employees ORDER BY salary DESC) WHERE ROWNUM <= 10`. Subquery zorunlu — ROWNUM ORDER BY'dan önce atanır. `SELECT * FROM employees WHERE ROWNUM <= 10 ORDER BY salary DESC` yanlış çalışır (sırasız 10 satır alır, sonra sıralar). Oracle 12c+: `FETCH FIRST 10 ROWS ONLY` — standart SQL. `OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY` — sayfalama.

**Q: CONNECT BY nedir? Recursive CTE'den farkı?**
A: CONNECT BY: Oracle'a özgü hiyerarşik sorgu. `START WITH manager_id IS NULL CONNECT BY PRIOR employee_id = manager_id`. `LEVEL` sözde kolonu ile derinlik. `CONNECT_BY_PATH` ile yol. Recursive CTE: ANSI SQL standart (`WITH RECURSIVE`) — PostgreSQL, MySQL, MSSQL'de çalışır. Oracle da hem CONNECT BY hem Recursive CTE destekler. CONNECT BY daha kısa ama Oracle'a özgü. Recursive CTE taşınabilir.

**Q: PL/SQL Package neden kullanılır?**
A: Encapsulation: private procedure/function dışarıdan görülmez (specification'da sadece public). State: Package değişkenleri session boyunca saklanır (fonksiyon çağrıları arası state). Performance: İlk çağrıda tüm package belleğe yüklenir, sonrakiler hızlı. Dependency management: Package body değişirse specification değişmeden recompile — bağımlı nesneler invalidate olmaz. Naming: farklı package'larda aynı isimli procedure olabilir.

**Q: BULK COLLECT ve FORALL neden hızlı?**
A: PL/SQL ↔ SQL arasında her satır için context switch maliyetlidir (row-by-row = slow-by-slow). BULK COLLECT: Tek SQL ile binlerce satırı PL/SQL koleksiyonuna al — context switch bir kez. `FETCH cursor BULK COLLECT INTO collection LIMIT 1000`. FORALL: Koleksiyondaki tüm satırlar için tek SQL çalıştır. `FORALL i IN 1..collection.COUNT INSERT INTO ...`. 10.000 satır: FORALL ~50ms, satır satır döngü ~5000ms.

**Q: Implicit vs Explicit cursor farkı?**
A: Implicit: Oracle her SELECT/DML için otomatik oluşturur. `SQL%ROWCOUNT`, `SQL%FOUND` ile kontrol. Tek satır için `SELECT INTO`. Birden fazla satır döner → `TOO_MANY_ROWS`. Explicit: Programcı açar, okur, kapar. `OPEN cursor; LOOP FETCH cursor INTO ...; EXIT WHEN cursor%NOTFOUND; END LOOP; CLOSE cursor`. Çok satır için gerekli. Cursor FOR LOOP: En kolay — açma/kapama otomatik.

**Q: PRAGMA EXCEPTION_INIT ne işe yarar?**
A: Oracle sistem exception'larına isim verir. `-1` kodu "ORA-00001: unique constraint violated" anlamına gelir. `PRAGMA EXCEPTION_INIT(e_dup_key, -1)` → artık named exception kullanılabilir. `EXCEPTION WHEN e_dup_key THEN ...` — `WHEN OTHERS THEN IF SQLCODE = -1 THEN ...` yerine daha okunabilir. Sık kullanılanlar: `-1` (unique), `-2292` (FK violation), `-1400` (NOT NULL).

**Q: MERGE komutu ne yapar? Neden yararlıdır?**
A: MERGE (Upsert): Kayıt varsa UPDATE, yoksa INSERT — tek SQL. `MERGE INTO target USING source ON (condition) WHEN MATCHED THEN UPDATE ... WHEN NOT MATCHED THEN INSERT ...`. Alternatifsiz: INSERT + UPDATE iki SQL ve transaction yönetimi. MERGE: atomik, tek SQL, deadlock riski düşük. Batch yükleme, staging tablosundan ana tabloya sync, ETL işlemlerinde yaygın. PostgreSQL karşılığı: `INSERT ... ON CONFLICT DO UPDATE`.

---

**Önceki →** [06 - PostgreSQL Advanced](../06_POSTGRESQL-ADVANCED)
**Sonraki →** [08 - MongoDB](../08_MONGODB)
