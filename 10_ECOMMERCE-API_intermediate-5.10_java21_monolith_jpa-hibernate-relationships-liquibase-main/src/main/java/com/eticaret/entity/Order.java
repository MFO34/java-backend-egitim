package com.eticaret.entity;

import com.eticaret.converter.OrderStatusConverter;
import com.eticaret.converter.PaymentStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ORDER ENTITY — Sipariş
 * =======================
 * İlişkiler:
 *   Order → User      : @ManyToOne  (FK: orders.user_id)
 *   Order → OrderItems: @OneToMany  (FK: order_items.order_id)
 *
 * Özel Özellikler:
 *   OrderStatus    : Sealed interface → @Convert ile String saklanır
 *   PaymentStatus  : Sealed interface → @Convert ile String saklanır
 *   @PrePersist    : Kayıt öncesi UUID sipariş numarası üret
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    // Müşteriye gösterilen benzersiz sipariş numarası: ORD-550e8400-e29b
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * @ManyToOne — KULLANICI
     * Birçok sipariş tek kullanıcıya ait.
     * FK: orders.user_id → users.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * @Convert(converter = OrderStatusConverter.class):
     *   autoApply = true olduğu için bu annotation gerekmez aslında.
     *   Açıkça yazmak okunabilirliği artırır.
     *   DB'de VARCHAR(30) olarak saklanır: "PENDING", "SHIPPED" vb.
     */
    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private OrderStatus status = new OrderStatus.Pending();

    // Ödeme durumu — PaymentStatusConverter otomatik devrede
    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "payment_status", length = 30, nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = new PaymentStatus.Pending();

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Teslimat adresi (sipariş anındaki adres snapshot'ı)
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * @OneToMany — SİPARİŞ KALEMLERİ
     * cascade = ALL: Sipariş kaydedilince kalemler de kaydedilir.
     * orphanRemoval = true: order.getItems().remove(item) → item silinir.
     */
    @OneToMany(mappedBy = "order",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * @PrePersist:
     *   Entity veritabanına ilk kez kaydedilmeden ÖNCE çalışır.
     *   Sipariş numarası: "ORD-" + UUID'nin ilk 8 karakteri
     *   Her sipariş benzersiz numara alır — otomatik olarak.
     */
    @PrePersist
    private void generateOrderNumber() {
        if (orderNumber == null) {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        }
    }

    // Sipariş durumunu güncelle — geçiş kurallarını kontrol et
    public void updateStatus(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Geçersiz durum geçişi: " + status.code() + " → " + newStatus.code());
        }
        this.status = newStatus;
    }

    // Toplam kalemleri sayfalı göster
    public int getTotalItemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }
}
