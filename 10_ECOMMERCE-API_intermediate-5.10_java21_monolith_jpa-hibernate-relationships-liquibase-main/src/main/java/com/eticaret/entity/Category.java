package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * CATEGORY ENTITY — HİYERARŞİK KATEGORİ YAPISI
 * ================================================
 * Self-join (Kendi kendine ilişki):
 *   Elektronik
 *   └── Akıllı Telefon
 *       └── iPhone
 *   └── Dizüstü Bilgisayar
 *
 * @ManyToOne parent: Üst kategori (null ise ana kategori)
 * @OneToMany children: Alt kategoriler
 *
 * SQL'de: categories tablosunda parent_id sütunu kendisine FK
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    // URL dostu isim: "Akıllı Telefon" → "akilli-telefon"
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * @ManyToOne — ÜST KATEGORİ (SELF-JOIN)
     * =========================================
     * Birçok alt kategori tek bir üst kategoriye bağlı.
     * FK: categories.parent_id → categories.id (aynı tablo!)
     *
     * FetchType.LAZY: parent yüklenmedikçe SQL atılmaz.
     * nullable = true: Ana kategoriler için parent_id null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Category parent;

    /**
     * @OneToMany — ALT KATEGORİLER (SELF-JOIN)
     * ==========================================
     * mappedBy = "parent": FK children entity'sindedir (parent field'ı).
     *
     * cascade = ALL: Üst kategori silinince alt kategoriler de silinir.
     * orphanRemoval = true: category.getChildren().remove(child) → child silinir.
     *
     * UYARI: Derin hiyerarşilerde N+1 sorunu!
     * Çözüm: @EntityGraph veya JOIN FETCH ile tüm ağacı tek sorguda çek.
     */
    @OneToMany(mappedBy = "parent",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    /**
     * @OneToMany — KATEGORİYE BAĞLI ÜRÜNLER
     * ========================================
     * cascade yok: Kategori silinince ürünler silinmemeli!
     * Ürünün category_id'si NULL yapılır (SET NULL).
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    // Yardımcı: alt kategori mi?
    public boolean isSubCategory() {
        return parent != null;
    }

    @Override
    public String toString() {
        return "Category{id=" + getId() + ", name='" + name + "'}";
    }
}
