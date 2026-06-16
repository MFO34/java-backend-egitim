package com.eticaret.entity;

/**
 * ORDER STATUS — JAVA 21 SEALED INTERFACE
 * =========================================
 * sealed: Sadece permits listesindeki sınıflar implement edebilir.
 * Bu sayede tüm olası durumlar derleme zamanında bilinir.
 *
 * Neden enum değil sealed interface?
 *   Enum: Sabit değerler, davranış eklemek zor
 *   Sealed Interface: Her durum kendi davranışını içerebilir
 *   Pattern Matching ile switch: exhaustive (tüm durumlar ele alınır)
 *
 * Veritabanında String olarak saklanır (AttributeConverter ile).
 *
 * Durum geçiş kuralları:
 *   PENDING → PROCESSING → SHIPPED → DELIVERED
 *   PENDING → CANCELLED
 *   PROCESSING → CANCELLED
 *   (Diğer geçişler geçersiz)
 */
public sealed interface OrderStatus
        permits OrderStatus.Pending,
                OrderStatus.Processing,
                OrderStatus.Shipped,
                OrderStatus.Delivered,
                OrderStatus.Cancelled,
                OrderStatus.Refunded {

    // Her durum kodunu döndürür (DB'ye yazılacak string)
    String code();

    // Kullanıcıya gösterilen Türkçe isim
    String displayName();

    // Terminal durum mu? (Sipariş bu durumda değiştirilemez)
    boolean isTerminal();

    // Bu durumdan belirtilen duruma geçiş yapılabilir mi?
    boolean canTransitionTo(OrderStatus next);

    // ===== PERMITTED RECORD IMPLEMENTATIONS =====
    // record: Java 21 — immutable, equals/hashCode/toString otomatik

    /**
     * PENDING — Ödeme bekleniyor
     * Geçiş: PROCESSING (ödeme geldi) veya CANCELLED
     */
    record Pending() implements OrderStatus {
        public String code() { return "PENDING"; }
        public String displayName() { return "Ödeme Bekleniyor"; }
        public boolean isTerminal() { return false; }
        public boolean canTransitionTo(OrderStatus next) {
            return next instanceof Processing || next instanceof Cancelled;
        }
    }

    /**
     * PROCESSING — Hazırlanıyor
     * Geçiş: SHIPPED veya CANCELLED
     */
    record Processing() implements OrderStatus {
        public String code() { return "PROCESSING"; }
        public String displayName() { return "Hazırlanıyor"; }
        public boolean isTerminal() { return false; }
        public boolean canTransitionTo(OrderStatus next) {
            return next instanceof Shipped || next instanceof Cancelled;
        }
    }

    /**
     * SHIPPED — Kargoya verildi
     * Geçiş: DELIVERED
     */
    record Shipped() implements OrderStatus {
        public String code() { return "SHIPPED"; }
        public String displayName() { return "Kargoda"; }
        public boolean isTerminal() { return false; }
        public boolean canTransitionTo(OrderStatus next) {
            return next instanceof Delivered;
        }
    }

    /**
     * DELIVERED — Teslim edildi (terminal)
     * Geçiş: REFUNDED (iade talebi)
     */
    record Delivered() implements OrderStatus {
        public String code() { return "DELIVERED"; }
        public String displayName() { return "Teslim Edildi"; }
        public boolean isTerminal() { return false; }
        public boolean canTransitionTo(OrderStatus next) {
            return next instanceof Refunded;
        }
    }

    /**
     * CANCELLED — İptal edildi (terminal)
     * Geri dönüş yok
     */
    record Cancelled() implements OrderStatus {
        public String code() { return "CANCELLED"; }
        public String displayName() { return "İptal Edildi"; }
        public boolean isTerminal() { return true; }
        public boolean canTransitionTo(OrderStatus next) { return false; }
    }

    /**
     * REFUNDED — İade edildi (terminal)
     */
    record Refunded() implements OrderStatus {
        public String code() { return "REFUNDED"; }
        public String displayName() { return "İade Edildi"; }
        public boolean isTerminal() { return true; }
        public boolean canTransitionTo(OrderStatus next) { return false; }
    }

    // ===== FACTORY METHOD =====
    /**
     * DB'deki String kodundan OrderStatus nesnesi oluştur.
     * AttributeConverter bu metodu çağırır.
     *
     * Pattern Matching for switch (Java 21):
     *   Tüm permitted tipler ele alınmazsa derleme hatası!
     *   Bu "exhaustiveness" sealed interface'in gücüdür.
     */
    static OrderStatus fromCode(String code) {
        return switch (code) {
            case "PENDING"    -> new Pending();
            case "PROCESSING" -> new Processing();
            case "SHIPPED"    -> new Shipped();
            case "DELIVERED"  -> new Delivered();
            case "CANCELLED"  -> new Cancelled();
            case "REFUNDED"   -> new Refunded();
            default -> throw new IllegalArgumentException("Bilinmeyen sipariş durumu: " + code);
        };
    }

    // ===== KULLANIM ÖRNEĞİ (service katmanında) =====
    /*
        OrderStatus status = order.getOrderStatus();

        // Pattern Matching for switch ile tip kontrolü
        String message = switch (status) {
            case Pending p    -> "Siparişiniz " + p.displayName() + " durumunda";
            case Processing p -> "Siparişiniz hazırlanıyor";
            case Shipped s    -> "Kargonuz yolda!";
            case Delivered d  -> "Teslim edildi, nasıl buldunuz?";
            case Cancelled c  -> "Sipariş iptal edildi";
            case Refunded r   -> "İade işleminiz tamamlandı";
        };
    */
}
