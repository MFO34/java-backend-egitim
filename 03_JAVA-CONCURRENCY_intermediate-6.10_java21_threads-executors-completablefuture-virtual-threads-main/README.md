# 03 — Java Concurrency
**Zorluk:** Intermediate (6/10) | **Java 21** | **Mülakat Hazırlığı**

Java 21 ile thread yönetimi, senkronizasyon, kilitleme mekanizmaları, asenkron programlama ve Virtual Threads.

---

## İçerik

| Konu | Dosya | Kapsanan Konular |
|------|-------|-----------------|
| **Thread Temelleri** | `basics/ThreadBasics.java` | Thread/Runnable/Callable, lifecycle, join, interrupt, daemon, priority, ThreadLocal |
| **Senkronizasyon** | `synchronization/SynchronizationDemo.java` | Race condition, synchronized, volatile, wait/notify, deadlock |
| **Locks** | `locks/LocksDemo.java` | ReentrantLock, tryLock, ReadWriteLock, StampedLock, Condition |
| **Executor Service** | `executors/ExecutorServiceDemo.java` | ThreadPool türleri, Future, invokeAll/Any, ScheduledExecutor, ThreadPoolExecutor |
| **CompletableFuture** | `async/CompletableFutureDemo.java` | thenApply/Compose/Combine, allOf, anyOf, exception handling, async pipeline |
| **Atomic** | `atomic/AtomicDemo.java` | AtomicInteger/Long/Reference/Boolean, CAS, LongAdder, ABA problemi |
| **Concurrent Collections** | `collections/ConcurrentCollectionsDemo.java` | ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueue, CountDownLatch, CyclicBarrier, Semaphore |
| **Virtual Threads** | `virtual/VirtualThreadsDemo.java` | Java 21 Project Loom, platform vs virtual karşılaştırma, Structured Concurrency, pinning |
| **Patterns** | `patterns/ProducerConsumer.java` | Producer-Consumer, BlockingQueue, CountDownLatch |

---

## Thread Lifecycle

```
       start()
NEW ─────────→ RUNNABLE ←──────────────────────┐
                  │                             │
                  │ CPU alır                    │ notify()/interrupt()
                  ↓                             │
              RUNNING ──────────────→ BLOCKED/WAITING/TIMED_WAITING
                  │
                  │ run() biter
                  ↓
             TERMINATED
```

---

## Senkronizasyon Araçları Karşılaştırması

```
Araç                | Kullanım Amacı                        | Özellik
--------------------+---------------------------------------+---------
synchronized        | Basit kritik bölge                    | Otomatik release
ReentrantLock       | Gelişmiş kilit (tryLock, timeout)      | Manuel unlock
ReadWriteLock       | Çok okuyucu / tek yazıcı              | Yüksek okuma performansı
StampedLock         | Optimistic read                       | En performanslı
volatile            | Görünürlük garantisi                  | Atomiklik yok
AtomicInteger/Long  | Lock-free sayaç                       | CAS tabanlı
CountDownLatch      | N görev bitene kadar bekle            | Tek kullanım
CyclicBarrier       | Thread'leri checkpoint'te buluştur    | Yeniden kullanılabilir
Semaphore           | Kaynak erişim sınırlama               | N permit
```

---

## Mülakat Soruları

**Q: Race condition nedir? Örnek ver.**
A: Birden fazla thread paylaşılan veriye eş zamanlı erişince, işlem sırasına bağlı beklenmedik sonuç. Örnek: `count++` = `read + increment + write` 3 adım. T1 okur (0), T2 okur (0), T1 yazar (1), T2 yazar (1). Gerçek değer 2 olmalıydı ama 1. Çözüm: `synchronized`, `AtomicInteger`, `volatile` (sadece görünürlük için).

**Q: volatile ile synchronized farkı nedir?**
A: `volatile`: Değişken cache'lenmesin, tüm thread'ler RAM'den okusun (görünürlük). Atomik olmayan işlemler için yetersiz: `count++` hâlâ race condition yaratır. `synchronized`: Hem görünürlük hem mutual exclusion. Blok içine bir anda sadece bir thread girer. Performans maliyeti yüksek. `volatile` sadece flag/sinyal için (`boolean running = true`), hesaplama için `synchronized`/`Atomic`.

**Q: Deadlock nasıl oluşur? Nasıl önlenir?**
A: İki thread birbirinin tuttuğu kilidi bekler. T1: kilit A alır → kilit B bekler. T2: kilit B alır → kilit A bekler. Önleme: (1) Kilit sırası — hep aynı sırada al (A sonra B, asla B sonra A). (2) `tryLock(timeout)` — bekleme süresi dolunca vazgeç. (3) Lock sayısını azalt — mümkünse tek lock. (4) Deadlock detection — `jstack` ile thread dump analizi.

**Q: ReentrantLock synchronized'dan ne zaman tercih edilir?**
A: `synchronized` basit, JVM optimize eder. `ReentrantLock` üstünlükleri: `tryLock()` — kilidi almaya çalış, başarısız olursa dönemez. `lockInterruptibly()` — bekleme sırasında interrupt edilebilir. `Condition` — birden fazla wait/notify koşulu. `fairness` — sıra garantisi. Genel kural: ihtiyaç yoksa `synchronized`, gelişmiş özellik gerekiyorsa `ReentrantLock`.

**Q: Future vs CompletableFuture farkı nedir?**
A: `Future`: `get()` ile sonucu al, ama bloklar! Callback yok, zincirleme yok, `cancel()` dışında kontrol yok. `CompletableFuture`: `thenApply()`, `thenCompose()`, `thenCombine()` ile zincirleme. `exceptionally()` ile hata yönetimi. `whenComplete()` callback. `CompletableFuture.allOf()` ile N future bekle. Non-blocking: `get()` yerine callback kullan. Modern kod: `CompletableFuture` her zaman tercih edilir.

**Q: ConcurrentHashMap neden HashMap'ten thread-safe'dir?**
A: Java 8+: Bucket başına bağımsız kilit (segment değil, her bucket için CAS). `get()`: lock almaz, volatile okuma. `put()`: sadece ilgili bucket kilitlenir — concurrent write farklı bucket'ta aynı anda. Boyut hesaplama: `LongAdder` ile atomic. Null key/value yasak (null → sentinel değer olarak kullanılamaz, NPE riski). `Collections.synchronizedMap(map)`: her işlemde tek kilit → düşük concurrency. `ConcurrentHashMap`: yüksek concurrency, segment kilitleme.

**Q: CAS (Compare and Swap) nedir? ABA problemi nasıl çözülür?**
A: CAS: `compareAndSet(expected, update)` — değer expected ise update ile değiştir, atomik. Lock olmadan thread-safe işlem. CPU instruction seviyesinde atomik. ABA problemi: T1 okur (A), T2 A→B→A yapar, T1 CAS yapar (A görür) — ara değişim fark edilmez. Çözüm: `AtomicStampedReference` — değer + stamp (versiyon) birlikte kontrol edilir. Stamp her güncelleme artar → A→B→A artık farklı stamp.

**Q: Virtual Thread nedir? Platform thread'den farkı?**
A: Platform thread: OS thread'ine map edilir. Stack: 1-2MB. Thread oluşturma pahalı (~100µs). 10K thread = 10-20GB RAM. Virtual thread (Java 21): JVM tarafından yönetilen hafif thread. Stack: birkaç KB. Milyonlarca oluşturulabilir. I/O'da block olunca platform thread'i bırakır, başka virtual thread çalışır. `Thread.ofVirtual().start(task)`. Uygun: I/O-bound (DB, HTTP). Uygun değil: CPU-bound (hesaplama).

**Q: synchronized virtual thread performansını neden düşürür (pinning)?**
A: Virtual thread I/O'da block olunca platform thread'ini bırakır — başkası kullanır. Ama `synchronized` blok içindeyken platform thread'ini BIRAKAMAAZ (pin edilmiş). Sistem thread'leri tükenir → throughput düşer. Çözüm: `ReentrantLock` kullan (virtual thread aware). Spring Boot 3.2+ ve JDBC sürücüleri virtual thread uyumlu hale getirildi. `-Djdk.tracePinnedThreads=full` ile tespiti yap.

---

## Çalıştırma

```bash
mvn compile

# Her demo ayrı çalıştırılabilir
mvn exec:java -Dexec.mainClass="com.concurrency.basics.ThreadBasics"
mvn exec:java -Dexec.mainClass="com.concurrency.synchronization.SynchronizationDemo"
mvn exec:java -Dexec.mainClass="com.concurrency.async.CompletableFutureDemo"
mvn exec:java -Dexec.mainClass="com.concurrency.virtual.VirtualThreadsDemo"
```

---

## Öğrenme Sırası

```
1. ThreadBasics          → Thread nedir, nasıl oluşturulur
2. SynchronizationDemo   → Race condition, synchronized, volatile
3. LocksDemo             → Gelişmiş kilit mekanizmaları
4. ExecutorServiceDemo   → Thread havuzu yönetimi
5. CompletableFutureDemo → Asenkron programlama
6. AtomicDemo            → Lock-free programlama
7. ConcurrentCollections → Thread-safe veri yapıları
8. VirtualThreadsDemo    → Java 21 yeni özellikler
```

---

**Önceki Repo →** [02 - Bank Account Manager](../02_BANK-ACCOUNT-MANAGER)
**Sonraki Repo →** [04 - Design Patterns](../04_DESIGN-PATTERNS)
