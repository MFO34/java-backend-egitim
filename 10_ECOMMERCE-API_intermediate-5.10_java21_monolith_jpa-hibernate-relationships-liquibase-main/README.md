# 🛒 E-Ticaret Ürün ve Sipariş Yönetim REST API

Spring Boot 3.3.x + JPA & Hibernate 6 + Liquibase ile geliştirilmiş,
JPA'nın **tüm ilişki tiplerini** ve **ileri düzey özelliklerini** gösteren pedagojik e-ticaret API'si.

---

## Veritabanı ER Diyagramı (ASCII)

```
┌──────────────┐       ┌──────────────┐
│   addresses  │       │    users     │
│──────────────│       │──────────────│
│ id (PK)      │◄──────│ id (PK)      │
│ title        │  1:1  │ first_name   │
│ street       │       │ last_name    │
│ city         │       │ email (UNIQ) │
│ district     │       │ password     │
│ postal_code  │       │ phone        │
│ country      │       │ address_id(FK)│
└──────────────┘       │ is_active    │
                       │ version      │
                       └──────┬───────┘
                              │ 1:N
              ┌───────────────┼──────────────────┐
              │               │                  │
              ▼               ▼                  ▼
         ┌─────────┐    ┌──────────┐      ┌──────────┐
         │ orders  │    │  carts   │      │ reviews  │
         │─────────│    │──────────│      │──────────│
         │ id (PK) │    │ id (PK)  │      │ id (PK)  │
         │order_num│    │ user_id  │      │ user_id  │
         │ user_id │    └────┬─────┘      │product_id│
         │ status  │         │ 1:N        │ rating   │
         │pay_stat │         ▼            │ content  │
         │ total   │    ┌──────────┐      └──────────┘
         │ version │    │cart_items│
         └────┬────┘    │──────────│
              │ 1:N     │ cart_id  │
              ▼         │product_id│
      ┌─────────────┐   │ quantity │
      │ order_items │   └──────────┘
      │─────────────│
      │ order_id    │
      │ product_id  │
      │ quantity    │
      │ unit_price  │
      └─────────────┘

┌──────────────────────────────────────────────────────┐
│                    products                          │
│──────────────────────────────────────────────────────│
│ id (PK)    name       slug (UNIQ)    description     │
│ price      stock_qty  is_active      status          │
│ category_id (FK)      version (@Version - Opt.Lock)  │
└───────┬──────────────────────────────────────────────┘
        │ N:1                    │ 1:N
        ▼                        ▼
┌──────────────┐         ┌──────────────────┐
│  categories  │         │  product_images  │
│──────────────│         │──────────────────│
│ id (PK)      │         │ id (PK)          │
│ name         │         │ product_id (FK)  │
│ slug         │         │ image_url        │
│ parent_id(FK)│◄─┐      │ is_primary       │
│  (self-join) │  │      └──────────────────┘
└──────────────┘  │ Self
                  │ Ref
                  └──── parent_id → id (hiyerarşi)

┌──────────────┐    ┌───────────────┐    ┌──────────┐
│   products   │    │  product_tags │    │   tags   │
│──────────────│    │───────────────│    │──────────│
│ id (PK)      │◄───│ product_id(FK)│───►│ id (PK)  │
│ ...          │    │ tag_id (FK)   │    │ name     │
└──────────────┘    └───────────────┘    │ slug     │
       @ManyToMany ara tablo              └──────────┘
```

---

## JPA ve Hibernate Nedir?

### JPA (Jakarta Persistence API)
JPA bir **spesifikasyondur** (arayüz) — SQL yazmadan Java nesnelerini veritabanına kaydetmeyi standartlaştırır.

```
Java Kodu                    Veritabanı
───────────────              ──────────────────
Product product = new        INSERT INTO products
  Product("iPhone",           (name, price, ...)
  54999.99, ...);             VALUES ('iPhone', ...)
repo.save(product);
```

### Hibernate
Hibernate, JPA'nın en popüler **implementasyonudur**. Spring Boot varsayılan olarak Hibernate kullanır.

```
JPA (Standart)       Hibernate (Uygulama)     PostgreSQL
──────────────       ────────────────────     ──────────
@Entity          →   EntityManagerFactory  →  SQL üretir
@OneToMany       →   LazyCollectionProxy   →  lazy select
@Transactional   →   Session yönetimi      →  commit/rollback
```

### ORM (Object-Relational Mapping) Nedir?
```
Java Dünyası              Veritabanı Dünyası
────────────────          ──────────────────
Sınıf (class)        ↔   Tablo (table)
Nesne (object)       ↔   Satır (row)
Alan (field)         ↔   Sütun (column)
Referans (reference) ↔   Yabancı Anahtar (foreign key)
```

---

## Tüm JPA İlişki Tipleri

### @OneToOne — Bire-Bir İlişki

```java
// User ↔ Address: Bir kullanıcının bir adresi var
// FK: users.address_id → addresses.id

@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@JoinColumn(name = "address_id")
private Address address;
```

```
users tablosu:                    addresses tablosu:
┌────┬──────────┬────────────┐    ┌────┬──────────────────┐
│ id │   email  │ address_id │    │ id │ city             │
├────┼──────────┼────────────┤    ├────┼──────────────────┤
│  1 │ahmet@... │     1      │───►│  1 │ İstanbul         │
│  2 │fatma@... │     2      │───►│  2 │ Ankara           │
└────┴──────────┴────────────┘    └────┴──────────────────┘
```

### @OneToMany / @ManyToOne — Bire-Çok İlişki

```java
// User → Orders: Bir kullanıcının birçok siparişi var
// FK: orders.user_id → users.id (FK çoğul tarafta)

// User tarafı (BIR):
@OneToMany(mappedBy = "user", cascade = {PERSIST, MERGE}, fetch = LAZY)
private List<Order> orders;

// Order tarafı (ÇOK):
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

```
users tablosu:          orders tablosu:
┌────┬──────────┐       ┌────┬─────────────┬─────────┐
│ id │  email   │       │ id │ order_number│ user_id │
├────┼──────────┤       ├────┼─────────────┼─────────┤
│  1 │ahmet@... │◄──┬───│  1 │ ORD-ABC123  │    1    │
│  2 │fatma@... │   ├───│  2 │ ORD-DEF456  │    1    │
└────┴──────────┘   └───│  3 │ ORD-GHI789  │    2    │
                        └────┴─────────────┴─────────┘
```

### @ManyToMany — Çoka-Çok İlişki

```java
// Product ↔ Tag: Bir ürün birçok etikete, bir etiket birçok ürüne sahip
// Ara tablo: product_tags (product_id, tag_id)

// Product tarafı (SAHIP — @JoinTable burada):
@ManyToMany(cascade = {PERSIST, MERGE}, fetch = LAZY)
@JoinTable(
    name = "product_tags",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private List<Tag> tags;

// Tag tarafı (AYNA — sadece okur):
@ManyToMany(mappedBy = "tags", fetch = LAZY)
private List<Product> products;
```

```
products:          product_tags:       tags:
┌────┬────────┐    ┌────────────┬──────────┐   ┌────┬───────────┐
│ id │ name   │    │ product_id │  tag_id  │   │ id │ name      │
├────┼────────┤    ├────────────┼──────────┤   ├────┼───────────┤
│  1 │iPhone  │    │     1      │    1     │──►│  1 │İndirimli  │
│  2 │Samsung │    │     1      │    3     │──►│  3 │Çok Satan  │
└────┴────────┘    │     2      │    2     │──►│  2 │Yeni Sezon │
                   └────────────┴──────────┘   └────┴───────────┘
```

### Self-Join (Kendi Kendine İlişki)

```java
// Category hiyerarşisi: Elektronik → Akıllı Telefon → iPhone
// Hem @ManyToOne (parent) hem @OneToMany (children) aynı tabloda!

@ManyToOne(fetch = LAZY)
@JoinColumn(name = "parent_id")
private Category parent;

@OneToMany(mappedBy = "parent", cascade = ALL)
private List<Category> children;
```

```
categories tablosu:
┌────┬─────────────────────┬───────────┐
│ id │ name                │ parent_id │
├────┼─────────────────────┼───────────┤
│  1 │ Elektronik          │   NULL    │  ← Ana kategori
│  4 │ Akıllı Telefon      │     1     │  ← Alt kategori
│  5 │ Dizüstü Bilgisayar  │     1     │  ← Alt kategori
│  3 │ Kitap               │   NULL    │  ← Ana kategori
│  6 │ Programlama         │     3     │  ← Alt kategori
└────┴─────────────────────┴───────────┘
```

---

## CascadeType Nedir? Farkları Neler?

`cascade`: Bir entity üzerindeki işlem, ilişkili entity'lere de yayılır mı?

| CascadeType | Ne Zaman Devreye Girer? | Kullanım Örneği |
|-------------|-------------------------|-----------------|
| `PERSIST`   | `entityManager.persist()` (save) | Yeni sipariş + kalemleri birlikte kaydet |
| `MERGE`     | `entityManager.merge()` (update) | Sipariş güncellenince kalemler de güncellenir |
| `REMOVE`    | `entityManager.remove()` (delete) | Kategori silinince ürünler de silinir |
| `REFRESH`   | `entityManager.refresh()` | DB'den yeniden yüklenince ilişkiler de yüklenir |
| `DETACH`    | `entityManager.detach()` | Session'dan ayrılınca ilişkiler de ayrılır |
| `ALL`       | Tüm yukarıdaki 5'i birden | User silinince Cart de silinsin |

```java
// ✅ Doğru cascade seçimi:
@OneToMany(cascade = {PERSIST, MERGE})  // Sipariş kaydedilince kalemler de kayıt
private List<OrderItem> items;          // AMA sipariş silinince kalemler SİLİNMEZ

@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)  // User silinince Cart de sil
private Cart cart;

// ❌ Yanlış:
@OneToMany(cascade = REMOVE)            // Kategori silinince tüm ürünler silinir — tehlikeli!
private List<Product> products;
```

### `orphanRemoval = true` nedir?
```java
// Fark: REMOVE vs orphanRemoval

// CascadeType.REMOVE: Parent silinince child silinir
parent.delete() → child silinir ✓

// orphanRemoval = true: İLİŞKİDEN ÇIKARILINCA silinir
parent.getItems().remove(item) → item DB'den silinir ✓
// CascadeType.REMOVE OLMADAN bu olmaz!

// Örnek:
cart.getItems().clear() → tüm CartItem satırları DB'den silinir (orphanRemoval=true sayesinde)
```

---

## LAZY vs EAGER Yükleme

| Özellik | LAZY | EAGER |
|---------|------|-------|
| Ne zaman yüklenir? | İlişkiye ilk erişildiğinde | Entity yüklenince hemen |
| SQL ne zaman çalışır? | `getChildren()` çağrıldığında | `findById()` çağrıldığında |
| Varsayılan | `@OneToMany`, `@ManyToMany` | `@ManyToOne`, `@OneToOne` |
| Performans | ✅ İyi (ihtiyaç varsa yükle) | ❌ Kötü (her zaman JOIN) |
| Risk | LazyInitializationException | Gereksiz veri yükleme |

```java
// LAZY — İdeal
@ManyToOne(fetch = FetchType.LAZY)
private Category category;
// product = findById(1)    → SELECT * FROM products WHERE id=1
// product.getCategory()   → SELECT * FROM categories WHERE id=? (sonradan)

// EAGER — Dikkatli kullan
@ManyToOne(fetch = FetchType.EAGER)
private Category category;
// product = findById(1)   → SELECT p.*, c.* FROM products p JOIN categories c ON ...
//                            (category ihtiyaç olmasa bile gelir!)

// LazyInitializationException nedir?
// @Transactional dışında lazy koleksiyona erişilirse olur:
@Transactional                    // Session açık → LAZY çalışır ✓
public List<Product> getAll() {
    return productRepo.findAll(); // products yüklendi
}
// Transaction kapandı, session kapandı
product.getCategory(); // LazyInitializationException! ❌

// Çözüm: open-in-view=false + service içinde erişim
```

---

## N+1 Problemi ve Çözümü

### Problem

```
// 100 ürünü listele → kategori adlarını da göster
List<Product> products = productRepository.findAll(); // 1 sorgu

for (Product p : products) {
    System.out.println(p.getCategory().getName()); // Her ürün için 1 sorgu!
}
// Toplam: 1 + 100 = 101 sorgu → N+1 Problemi!
```

```sql
-- Üretilen SQL (kötü):
SELECT * FROM products;                          -- 1 sorgu
SELECT * FROM categories WHERE id = 4;           -- ürün 1 için
SELECT * FROM categories WHERE id = 4;           -- ürün 2 için (aynı kategori!)
SELECT * FROM categories WHERE id = 5;           -- ürün 3 için
-- ... 97 sorgu daha
```

### Çözüm 1: @EntityGraph

```java
// Repository'de:
@EntityGraph(attributePaths = {"category", "images"})
Page<Product> findAllByIsActiveTrue(Pageable pageable);

// Üretilen SQL (iyi):
SELECT p.*, c.*, i.*
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN product_images i ON i.product_id = p.id
WHERE p.is_active = true
-- 1 SORGU! Hepsi geldi.
```

### Çözüm 2: JOIN FETCH (JPQL)

```java
@Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN FETCH p.category
    LEFT JOIN FETCH p.images
    WHERE p.isActive = true
    """)
List<Product> findAllWithDetails();
```

### Çözüm 3: @BatchSize (Hibernate)

```java
// Hibernate birden fazla ID'yi tek sorguda çeker
@BatchSize(size = 20)
@OneToMany(mappedBy = "product")
private List<Review> reviews;

// Üretilen SQL:
SELECT * FROM reviews WHERE product_id IN (1, 2, 3, ... 20)
// 100 ürün için: 100 / 20 = 5 sorgu (N+1 yerine!)
```

### application.yml'de Global @BatchSize

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 20  # Tüm koleksiyonlar için varsayılan
```

---

## Liquibase Nedir ve Nasıl Kullanılır?

### Liquibase Nedir?
Liquibase, veritabanı şemasını **kod gibi yönetir** (Database as Code).

```
Geleneksel Yöntem          Liquibase
──────────────────         ──────────────────────────
SQL el ile çalıştır    →   XML/YAML dosyası yaz
"Hangi script çalıştı?" →  DATABASECHANGELOG kaydeder
Prodüksiyonda farklı   →   Tüm ortamlarda aynı
Geri almak zor         →   rollback komutu var
```

### Çalışma Mantığı

```
Uygulama başlar
      ↓
Liquibase: DATABASECHANGELOG tablosuna bak
      ↓
Hangi changeset'ler daha önce çalışmadı?
      ↓
Yeni changeset'leri sırayla çalıştır
      ↓
Başarılı olanları DATABASECHANGELOG'a kaydet
      ↓
Uygulama devam eder
```

### DATABASECHANGELOG Tablosu

```sql
SELECT * FROM DATABASECHANGELOG;

-- id                  | author      | filename                    | dateExecuted
-- 001-create-users    | eticaret-dev| changes/001-create-tables   | 2024-01-15 10:30:00
-- 001-create-addresses| eticaret-dev| changes/001-create-tables   | 2024-01-15 10:30:01
-- 002-insert-categories| eticaret-dev| changes/002-insert-sample  | 2024-01-15 10:30:02
```

### Changeset Örneği

```xml
<!-- Her changeset: id + author ile benzersiz tanımlanır -->
<changeSet id="001-create-products" author="eticaret-dev">

    <createTable tableName="products">
        <column name="id" type="BIGSERIAL">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="name" type="VARCHAR(300)">
            <constraints nullable="false"/>
        </column>
        <!-- @Version için version sütunu -->
        <column name="version" type="BIGINT" defaultValueNumeric="0"/>
    </createTable>

    <!-- Rollback: Bu changeset geri alınırsa ne yapılsın? -->
    <rollback>
        <dropTable tableName="products"/>
    </rollback>
</changeSet>
```

### Liquibase Komutları

```bash
# Bekleyen migration'ları çalıştır (uygulama başında otomatik yapılır)
./mvnw liquibase:update

# Rollback: Son 1 changeset'i geri al
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1

# Tag'e kadar geri al
./mvnw liquibase:rollback -Dliquibase.rollbackTag=v1.0

# Çalışmış changeset'leri göster
./mvnw liquibase:history

# Bekleyen changeset'leri göster (çalıştırmadan)
./mvnw liquibase:status

# SQL önizleme (gerçekte çalıştırmadan)
./mvnw liquibase:updateSQL
```

---

## Specification Pattern Nedir?

### Problem: Kombinatöryal Patlama

```java
// Tüm filtre kombinasyonları için metod gerekir — imkansız!
findByKeyword(String keyword);
findByCategory(Long categoryId);
findByKeywordAndCategory(String keyword, Long categoryId);
findByKeywordAndCategoryAndMinPrice(String keyword, Long cat, BigDecimal min);
// ... 2^5 = 32 metod!
```

### Çözüm: Specification Pattern

```java
// Her filtre = ayrı Specification (JPA Predicate)
var spec = Specification
    .where(ProductSpecification.isActive())              // Her zaman
    .and(ProductSpecification.hasKeyword("telefon"))      // keyword varsa
    .and(ProductSpecification.hasCategory(1L))            // category varsa
    .and(ProductSpecification.hasPriceBetween(
        new BigDecimal("1000"),
        new BigDecimal("50000")));                        // fiyat aralığı varsa

// JpaSpecificationExecutor.findAll() ile çalıştır
Page<Product> results = productRepo.findAll(spec, pageable);
```

### Specification Nasıl Çalışır?

```java
// Her Specification = (Root, Query, CriteriaBuilder) → Predicate
public static Specification<Product> hasKeyword(String keyword) {
    return (root, query, cb) -> {
        if (keyword == null) return null;  // Filtre uygulanmaz

        var pattern = "%" + keyword.toLowerCase() + "%";
        return cb.or(
            cb.like(cb.lower(root.get("name")), pattern),        // name LIKE
            cb.like(cb.lower(root.get("description")), pattern)  // OR description LIKE
        );
    };
}
// Üretilen SQL:
// WHERE (LOWER(p.name) LIKE '%telefon%' OR LOWER(p.description) LIKE '%telefon%')
```

### API Kullanımı

```bash
# Stokta olan, 1000-50000 TL arası "telefon" arama
GET /api/v1/products/filter?keyword=telefon&minPrice=1000&maxPrice=50000&inStockOnly=true

# İndirimde olan Elektronik kategorisi ürünleri
GET /api/v1/products/filter?categoryId=1&onSaleOnly=true

# "Çok Satan" etiketli ürünler
GET /api/v1/products/filter?tagSlug=cok-satan&page=0&size=20&sort=price,asc
```

---

## Tüm API Endpoint Listesi

Base URL: `http://localhost:8080/api/v1`

### Kullanıcılar (`/users`)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `POST` | `/users` | Yeni kullanıcı oluştur (sepet otomatik açılır) |
| `GET` | `/users/{id}` | Kullanıcı getir |
| `GET` | `/users` | Tüm kullanıcılar |
| `DELETE` | `/users/{id}` | Kullanıcıyı pasife al |

### Kategoriler (`/categories`)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `POST` | `/categories` | Yeni kategori (parentId ile alt kategori) |
| `GET` | `/categories` | Hiyerarşik liste (JOIN FETCH — N+1 çözümü) |
| `GET` | `/categories/{id}` | Kategori detayı |
| `DELETE` | `/categories/{id}` | Soft delete |

### Ürünler (`/products`)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `POST` | `/products` | Ürün oluştur |
| `GET` | `/products` | Sayfalı liste (@EntityGraph) |
| `GET` | `/products/filter` | Dinamik filtrele (Specification) |
| `GET` | `/products/best-selling?limit=10` | En çok satanlar |
| `GET` | `/products/{id}` | ID ile detay |
| `GET` | `/products/slug/{slug}` | Slug ile detay |
| `PUT` | `/products/{id}` | Güncelle (PATCH davranışı) |
| `DELETE` | `/products/{id}` | Soft delete |

**Filtre parametreleri:**
```
keyword     → Ad/açıklama arama
categoryId  → Kategori ID
minPrice    → Minimum fiyat
maxPrice    → Maksimum fiyat
tagSlug     → Etiket slug
inStockOnly → true/false
onSaleOnly  → true/false
page, size, sort → Sayfalama
```

### Sepet (`/cart`)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `GET` | `/cart/user/{userId}` | Sepeti görüntüle |
| `POST` | `/cart/user/{userId}/items` | Ürün ekle |
| `DELETE` | `/cart/user/{userId}/items/{productId}` | Ürün çıkar |
| `DELETE` | `/cart/user/{userId}` | Sepeti boşalt |

### Siparişler (`/orders`)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `POST` | `/orders` | Sepetten sipariş oluştur |
| `GET` | `/orders/{id}` | Sipariş detayı |
| `GET` | `/orders/user/{userId}` | Kullanıcı siparişleri |
| `PATCH` | `/orders/{id}/status` | Durum güncelle (sealed interface kuralları) |
| `POST` | `/orders/{id}/cancel` | Siparişi iptal et (stok geri verilir) |

**Geçerli durum geçişleri:**
```
PENDING → PROCESSING → SHIPPED → DELIVERED → REFUNDED
PENDING → CANCELLED
PROCESSING → CANCELLED
```

### Yorumlar (`/reviews`)

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `POST` | `/reviews` | Yorum ekle (1 kullanıcı = 1 yorum) |
| `GET` | `/reviews/product/{productId}` | Ürün yorumları |
| `PATCH` | `/reviews/{id}/approve` | Yorumu onayla |
| `DELETE` | `/reviews/{id}` | Yorumu sil |

---

## Docker ile Çalıştırma

```bash
# 1. Ortam dosyasını hazırla
cp .env.example .env

# 2. Tüm stack'i başlat (db + app + pgadmin)
docker-compose up --build

# 3. Arka planda çalıştır
docker-compose up --build -d

# 4. Logları takip et
docker-compose logs -f app

# 5. Adreslere eriş
# API:        http://localhost:8080/api/v1
# Swagger:    http://localhost:8080/swagger-ui.html
# pgAdmin:    http://localhost:5050
```

**pgAdmin bağlantı ayarları:**
- Host: `db` (docker servis adı)
- Port: `5432`
- Database: `eticaret_db`
- Username: `eticaret_user`
- Password: `eticaret_pass`

---

## Örnek API İstekleri (curl)

### Kullanıcı Oluştur
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ahmet","lastName":"Yılmaz","email":"ahmet@test.com","password":"sifre123"}'
```

### Kategori Oluştur
```bash
# Ana kategori
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Elektronik","slug":"elektronik"}'

# Alt kategori (parentId ile)
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Akıllı Telefon","slug":"akilli-telefon","parentId":1}'
```

### Ürün Oluştur (@ManyToMany etiketlerle)
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "slug": "iphone-15-pro",
    "price": 54999.99,
    "originalPrice": 59999.99,
    "stockQuantity": 50,
    "categoryId": 2,
    "tagNames": ["İndirimli", "Çok Satan"]
  }'
```

### Dinamik Filtreleme (Specification Pattern)
```bash
# Stokta olan, 10000-60000 TL, "iPhone" içeren
curl "http://localhost:8080/api/v1/products/filter?keyword=iPhone&minPrice=10000&maxPrice=60000&inStockOnly=true&page=0&size=10"
```

### Sepete Ekle → Sipariş Ver
```bash
# Sepete ekle
curl -X POST http://localhost:8080/api/v1/cart/user/1/items \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# Siparişe çevir
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"shippingAddress":"Atatürk Cad. No:1, Kadıköy/İstanbul"}'
```

### Sipariş Durumu Güncelle (Sealed Interface)
```bash
curl -X PATCH http://localhost:8080/api/v1/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"statusCode":"PROCESSING"}'
```

---

## Optimistic Locking (@Version) — Eş Zamanlı Stok Güncelleme

```java
// Product entity'de:
@Version
private Long version;  // 0 → 1 → 2 → ...

// İki kullanıcı aynı anda sipariş veriyor:
// Kullanıcı A: version=5, stok=1
// Kullanıcı B: version=5, stok=1

// Kullanıcı A önce kaydettiğinde:
UPDATE products SET stock=0, version=6 WHERE id=1 AND version=5;  -- Başarılı!

// Kullanıcı B sonradan kaydetmeye çalıştığında:
UPDATE products SET stock=0, version=6 WHERE id=1 AND version=5;  -- 0 satır etkilendi!
-- → OptimisticLockingFailureException fırlatılır
// GlobalExceptionHandler → 409 Conflict yanıtı
```

---

## Öğrenilen Kavramlar

**JPA Temel:**
`@Entity` `@Table` `@Column` `@Id` `@GeneratedValue` `@Enumerated` `@Lob` `@Transient`

**JPA İlişkiler:**
`@OneToOne` `@OneToMany` `@ManyToOne` `@ManyToMany` `@JoinColumn` `@JoinTable` `mappedBy`
`CascadeType` `FetchType.LAZY/EAGER` `orphanRemoval`

**Spring Data JPA:**
Method Naming · `@Query` JPQL · Native SQL · `@Modifying` · `@EntityGraph`
`JpaSpecificationExecutor` · `Pageable/Page` · Interface Projection

**Advanced JPA:**
`@Version` Optimistic Locking · `@MappedSuperclass` · JPA Auditing
`@CreatedDate` `@LastModifiedDate` `@CreatedBy` · `AttributeConverter`

**Java 21:**
`sealed interface` (OrderStatus, PaymentStatus) · Records (DTO'lar)
Pattern Matching for switch · Text Blocks (JPQL) · Virtual Threads

**Liquibase:**
`changeSet` · `DATABASECHANGELOG` · rollback · tag · XML migration

**Mimari Patternler:**
Specification Pattern · N+1 Çözümü · Soft Delete · DTO Pattern

---

## Mülakat Soruları

**Q: JPA'da CascadeType türleri nelerdir ve ne zaman kullanılır?**
A: `PERSIST` — parent kaydedilince child da kaydedilir. `MERGE` — parent güncellenince child da güncellenir. `REMOVE` — parent silinince child da silinir (dikkatli kullan!). `ALL` — hepsi. `DETACH` — parent detach olunca child da. `REFRESH` — parent DB'den yenilenince child da. Örnek: Ürün → Görseller: `cascade = ALL, orphanRemoval = true`. Görselsiz ürün olamaz. Ürün silinince görseller de silinir. REMOVE dikkat: `@ManyToMany`'de `cascade = REMOVE` kullanma — tag silinince diğer ürünlerden de tag kaybolur!

**Q: orphanRemoval ile cascade REMOVE farkı nedir?**
A: `cascade = REMOVE`: Parent entity `.remove()` ile silinince child'lar da silinir. `orphanRemoval = true`: Parent'tan ayrılan child otomatik silinir. Örnek: `product.getImages().remove(img)` → `orphanRemoval` ile img DB'den silinir, `cascade = REMOVE` olmasa da. İkisi birlikte kullanılabilir. `orphanRemoval` genellikle bire-çok ilişkilerde tercih edilir.

**Q: @MappedSuperclass ile @Inheritance(SINGLE_TABLE / JOINED / TABLE_PER_CLASS) farkı?**
A: `@MappedSuperclass`: Kendi tablosu yok, alanları alt sınıfların tablolarına eklenir. `BaseEntity` (id, createdAt, version) için ideal. `@Inheritance(SINGLE_TABLE)`: Tüm alt sınıflar tek tabloda, kullanılmayan alanlar NULL — sorgular hızlı ama NULL kirliği. `@Inheritance(JOINED)`: Her alt sınıf kendi tablosunda, JOIN ile birleştirme — veri tutarlılığı iyi, sorgu maliyeti yüksek. `@Inheritance(TABLE_PER_CLASS)`: Her alt sınıf tam bağımsız tablo — polymorphic query zor. Seçim: SINGLE_TABLE (basitlik + performans) veya JOINED (tutarlılık + kısıtlar).

**Q: Liquibase changeSet ID çakışırsa ne olur?**
A: `DATABASECHANGELOG` tablosu her çalıştırılan changeset'in checksum'ını saklar. Aynı ID'ye sahip changeset içeriği değişmişse → checksum hatası → uygulama başlamaz. Çözüm: Mevcut changeSet'leri ASLA düzenleme. Değişiklik için yeni changeSet ekle. `validCheckSum` ile eski checksum'ı kabul ettirilebilir (kaçınılması gereken workaround). Production'da rollback: `mvn liquibase:rollbackCount -Dliquibase.rollbackCount=1`.

**Q: Specification Pattern neden tercih edilir?**
A: Dinamik filtreleme için JPQL metodları veya if-else ile SQL birleştirmek yerine: her filtre bir `Specification<T>` (Predicate üretici). `Specification.where(byKeyword).and(byCategory).and(byPriceRange)` — sadece null olmayan filtreler eklenir. `JpaSpecificationExecutor.findAll(spec, pageable)` SQL üretir. Avantajlar: Her filtre izole test edilebilir, yeni filtre eklemek sadece yeni Specification sınıfı, tip güvenli. `@Query` ile uzun dinamik string birleştirme yerine okunabilir ve test edilebilir kod.

**Q: Interface Projection nedir? DTO'dan farkı?**
A: Interface Projection: Repository metodunun dönüş tipi bir interface. Spring Data, bu interface'i implement eden proxy üretir — sadece interface'de tanımlı getter'lar için SQL SELECT üretir. Tam entity yüklenmez → performanslı. Örnek: `ProductProjection` sadece `getId()`, `getName()`, `getPrice()` tanımlar → `SELECT id, name, price FROM products`. DTO Projection: `new ProductSummaryDTO(p.id, p.name, p.price)` JPQL constructor expression. DTO daha esnek (metod eklenebilir), Interface Projection daha az kod.

**Q: @EntityGraph nasıl N+1 problemini çözer?**
A: LAZY ilişkide her eleman için ayrı SQL atılır. `@EntityGraph(attributePaths = {"category", "images"})` ile repository metoduna eklenir. Hibernate tek bir JOIN SELECT üretir: `SELECT p, c, i FROM Product p LEFT JOIN FETCH p.category c LEFT JOIN FETCH p.images i`. N+1 yerine 1 sorgu. Dikkat: images `@OneToMany` ise JOIN FETCH ile kartezyen çarpım olabilir — `@BatchSize(size=25)` alternatif. Birden fazla collection JOIN FETCH: `MultipleBagFetchException` → ayrı sorgu gerekir.

**Q: sealed interface neden normal interface'den üstündür (OrderStatus için)?**
A: Normal interface: bilinmeyen implementasyonlar — switch exhaustive olmaz, default gerekir, runtime hatası riski. `sealed interface OrderStatus permits Pending, Processing, Shipped, Delivered, Cancelled` → sadece belirtilen sınıflar implement edebilir. `switch (status)` exhaustive: tüm alt sınıflar ele alınmadıysa compile hatası. Yeni durum eklenmek istenirse switch ifadelerini güncelle hatırlatması alırsın. Fintech, e-ticaret gibi state machine gerektiren durumlar için mükemmel.

**Q: AttributeConverter ne işe yarar?**
A: Java tipi ↔ DB tipi dönüşümü. `OrderStatusConverter`: DB'de `"PENDING"`, `"PROC"` gibi kısaltmalar olduğunda Java enum'a dönüştürür. `@Converter(autoApply=true)` → tüm entity'lerde o tip otomatik dönüştürülür. `@Convert(converter=OrderStatusConverter.class)` → sadece o field. Kullanım: JSON serileştirme, enum kısaltma, legacy DB uyumluluğu, şifreli alan (EncryptedConverter).
