package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * PRODUCT ENTITY — JPA'nın neredeyse tüm özelliklerini içerir
 * =============================================================
 * İlişkiler:
 *   Product → Category   : @ManyToOne  (FK: products.category_id)
 *   Product → Images     : @OneToMany  (FK: product_images.product_id)
 *   Product ↔ Tags       : @ManyToMany (Ara tablo: product_tags)
 *   Product → Reviews    : @OneToMany  (FK: reviews.product_id)
 *
 * Özel JPA Özellikleri:
 *   @Version      → Optimistic Locking (eş zamanlı stok güncelleme)
 *   @Lob          → Uzun açıklama metni (TEXT)
 *   @Transient    → DB'ye yazılmayan hesaplama alanı
 *   @Enumerated   → Java enum → String DB'de
 */
@Entity
@Table(name = "products",
    indexes = {
        @Index(name = "idx_products_slug",     columnList = "slug",        unique = true),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_price",    columnList = "price"),
        @Index(name = "idx_products_active",   columnList = "is_active")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 300)
    private String name;

    // URL dostu benzersiz isim: "iPhone 15 Pro" → "iphone-15-pro"
    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    /**
     * @Lob — BÜYÜK METİN ALANI
     * ==========================
     * Normal @Column → VARCHAR(255) ile sınırlı.
     * @Lob → PostgreSQL'de TEXT tipine map edilir (sınırsız uzunluk).
     * Ürün açıklaması binlerce karakter olabilir → TEXT gerekli.
     *
     * Dikkat: @Lob EAGER yüklenir! Büyük metinler performansı etkiler.
     * Çözüm: @Basic(fetch = FetchType.LAZY) — ama Hibernate desteği sınırlı.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    // BigDecimal: Para birimleri için Double yerine BigDecimal kullan!
    // Double: 54999.99 → 54999.98999999... (ondalık hata)
    // BigDecimal: tam kesin aritmetik
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // İndirimli ürün için orijinal fiyat
    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * @Enumerated(EnumType.STRING):
     *   Java enum'u veritabanında metin olarak sakla.
     *   EnumType.ORDINAL: sayı olarak saklar (0,1,2...) — KULLANMA!
     *     Sıralama değişirse DB verisi bozulur.
     *   EnumType.STRING: "ACTIVE","INACTIVE" → okunabilir, güvenli.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    // ===== İLİŞKİLER =====

    /**
     * @ManyToOne — KATEGORİ (FK: products.category_id)
     * ==================================================
     * FetchType.LAZY: Ürün yüklenince kategori yüklenmez.
     *   Sadece product.getCategory() çağrılınca SQL atılır.
     *   Listede 100 ürün var → 100 ayrı kategori sorgusu = N+1!
     *   Çözüm: @EntityGraph veya JOIN FETCH (repository'de gösterilir)
     *
     * optional = true: Kategori olmayan ürün olabilir (category_id NULL).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    /**
     * @OneToMany — ÜRÜN GÖRSELLERİ
     * ================================
     * cascade = ALL: Ürün kaydedilince görseller de kaydedilir.
     * orphanRemoval = true: product.getImages().remove(img) → img silinir.
     * FetchType.LAZY: Görseller sadece istenince yüklenir.
     */
    @OneToMany(mappedBy = "product",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    /**
     * @ManyToMany — ETİKETLER
     * ========================
     * Bu taraf ilişkinin "sahibidir" — @JoinTable burada tanımlanır.
     *
     * @JoinTable:
     *   name = "product_tags"       → ara tablonun adı
     *   joinColumns: Bu entity'nin FK'si (product_id)
     *   inverseJoinColumns: Karşı entity'nin FK'si (tag_id)
     *
     * cascade = {PERSIST, MERGE}:
     *   Yeni tag oluştururken ürünle birlikte kaydedilebilir.
     *   REMOVE yok: Ürün silinince tag'ler silinmemeli!
     *   Sadece ara tablodaki kayıt silinir (CascadeType.REMOVE bunu yapar).
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE},
                fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_tags",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    /**
     * @OneToMany — YORUMLAR
     * cascade yok: Ürün silinince yorumlar silinir (onDelete CASCADE DB'de)
     */
    @OneToMany(mappedBy = "product",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    /**
     * @Transient — VERİTABANINA YAZILMAZ
     * =====================================
     * Bu alan DB'de bir sütun DEĞILDIR.
     * Hesaplanmış değer: yorumların ortalaması.
     * Servis katmanında doldurulur veya @Formula ile hesaplanır.
     *
     * Neden? Ortalama her sorguda hesaplamak yerine burada sakla.
     * Cache veya hesaplama için kullanılır.
     */
    @Transient
    private Double averageRating;

    // Stok durumu kontrolü
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    // İndirimde mi?
    public boolean isOnSale() {
        return originalPrice != null && originalPrice.compareTo(price) > 0;
    }

    // Etiket ekle (bidirectional sync)
    public void addTag(Tag tag) {
        tags.add(tag);              // Bu tarafı güncelle
        tag.getProducts().add(this); // Karşı tarafı da güncelle
    }

    // Etiket kaldır (bidirectional sync)
    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getProducts().remove(this);
    }

    @Override
    public String toString() {
        return "Product{id=" + getId() + ", name='" + name + "', price=" + price + "}";
    }

    // ===== INNER ENUM =====
    public enum ProductStatus {
        ACTIVE,    // Aktif, satışta
        INACTIVE,  // Pasif, satışta değil
        ARCHIVED   // Arşivlenmiş
    }
}
