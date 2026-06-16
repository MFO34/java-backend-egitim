package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * ADDRESS ENTITY — @OneToOne ilişkinin "sahip olmayan" tarafı
 * ============================================================
 * User → Address: Bir kullanıcının bir adresi var.
 * FK (address_id) users tablosunda tutulur.
 * Bu entity sadece tablo eşleşmesidir, ilişki User'da tanımlıdır.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;         // "Ev Adresi", "İş Adresi"

    @Column(nullable = false, length = 255)
    private String street;        // "Atatürk Cad. No:1 Daire:5"

    @Column(nullable = false, length = 100)
    private String city;          // "İstanbul"

    @Column(length = 100)
    private String district;      // "Kadıköy"

    @Column(name = "postal_code", length = 20)
    private String postalCode;    // "34710"

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String country = "Türkiye";

    /**
     * @OneToOne(mappedBy = "address"):
     *   Bu taraf ilişkinin "ayna" tarafıdır.
     *   FK users tablosundadır, burada tutulmaz.
     *   mappedBy = "address" → User sınıfındaki "address" field'ını işaret eder.
     *
     * Neden burada User referansı?
     *   Address → User'a erişmek gerekirse (çift yönlü ilişki).
     *   Eğer tek yönlü yeterli → bu field yazılmayabilir.
     */
    @OneToOne(mappedBy = "address", fetch = FetchType.LAZY)
    private User user;

    @Override
    public String toString() {
        // toString'de lazy koleksiyonlara erişme → LazyInitializationException!
        return title + " - " + street + ", " + district + "/" + city;
    }
}
