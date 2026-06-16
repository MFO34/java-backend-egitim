package com.eticaret.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * USER ENTITY — @OneToOne, @OneToMany ilişkiler
 * ===============================================
 * İlişkiler:
 *   User ↔ Address  : @OneToOne  (FK: users.address_id)
 *   User → Orders   : @OneToMany (FK: orders.user_id)
 *   User ↔ Cart     : @OneToOne  (FK: carts.user_id)
 *   User → Reviews  : @OneToMany (FK: reviews.user_id)
 */
@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    // unique = true: aynı e-posta iki kez kayıt olamaz
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * @Lob (Large Object):
     *   Uzun metinler için TEXT tipine map edilir.
     *   Normal @Column → VARCHAR(255) ile sınırlı.
     *   Şifre hash'i genellikle 60-100 karakter ama güvenli olmak için TEXT.
     *   UYARI: @Lob eager loading yapar — dikkatli kullan.
     */
    @Lob
    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * @OneToOne — TEK YÖNDE İLİŞKİ (FK bu tabloda)
     * =============================================
     * cascade = CascadeType.ALL:
     *   User kaydedilince Address de kaydedilir (PERSIST)
     *   User güncellenince Address de güncellenir (MERGE)
     *   User silinince Address de silinir (REMOVE)
     *   ALL = PERSIST + MERGE + REMOVE + REFRESH + DETACH
     *
     * orphanRemoval = true:
     *   user.setAddress(null) yapılırsa Address tablosundan da silinir.
     *   cascade REMOVE ile fark: orphanRemoval koleksiyondan çıkarma durumunu da kapsar.
     *
     * FetchType.LAZY:
     *   user.getAddress() çağrılmadıkça SQL sorgusu atılmaz.
     *   SELECT user JOIN address → yavaş (her user için address de gelir)
     *   LAZY → sadece gerektiğinde yüklenir → performanslı
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    /**
     * @OneToMany — BİR KULLANICININ SİPARİŞLERİ
     * ===========================================
     * mappedBy = "user":
     *   FK Order entity'sindedir (orders.user_id).
     *   mappedBy ile "sahiplik" Order tarafındadır.
     *   Bu taraf ilişkiyi yönetmez — sadece okur.
     *
     * cascade = {PERSIST, MERGE}:
     *   CascadeType.REMOVE YOK! Kullanıcı silinirse siparişleri silinmemeli.
     *   Sadece PERSIST ve MERGE yeterli.
     *
     * FetchType.LAZY (OneToMany için varsayılan):
     *   Kullanıcı yüklenince siparişler gelmez.
     *   user.getOrders() çağrılınca ayrı SELECT çalışır.
     */
    @OneToMany(mappedBy = "user",
               cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    /**
     * @OneToOne (Çift Yönlü — FK Cart tarafında)
     * ============================================
     * mappedBy = "user": FK carts.user_id'de
     * cascade ALL + orphanRemoval: User silinince Cart de silinir
     */
    @OneToOne(mappedBy = "user",
              cascade = CascadeType.ALL,
              orphanRemoval = true,
              fetch = FetchType.LAZY)
    private Cart cart;

    /**
     * @OneToMany — Yorumlar
     * fetch = LAZY: Kullanıcı yüklenince yorumlar gelmez
     */
    @OneToMany(mappedBy = "user",
               cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    // Yardımcı metod: tam isim
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        // Lazy koleksiyonlara erişme!
        return "User{id=" + getId() + ", email='" + email + "'}";
    }
}
