package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TAG ENTITY — @ManyToMany ilişkinin "sahip olmayan" tarafı
 * ===========================================================
 * Ürünler birden fazla etikete sahip olabilir.
 * Bir etiket birden fazla üründe kullanılabilir.
 * → ManyToMany ilişki
 *
 * Ara tablo: product_tags (product_id, tag_id)
 * @JoinTable Product entity'sinde tanımlıdır (sahip taraf).
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;   // "İndirimli", "Yeni Sezon"

    @Column(nullable = false, unique = true, length = 100)
    private String slug;   // "indirimli", "yeni-sezon"

    /**
     * @ManyToMany (mappedBy = "tags"):
     *   Sahip taraf Product entity'sindeki "tags" field'ıdır.
     *   @JoinTable orada tanımlıdır.
     *   Bu taraf sadece okuma yapar — ara tabloya müdahale etmez.
     *
     * FetchType.LAZY: Tag yüklenince ürünler yüklenmez.
     *   EAGER olsaydı: Her tag sorgusu → tüm ürünleri çekiyor (yavaş!)
     */
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @Override
    public String toString() {
        return "Tag{id=" + getId() + ", name='" + name + "'}";
    }
}
