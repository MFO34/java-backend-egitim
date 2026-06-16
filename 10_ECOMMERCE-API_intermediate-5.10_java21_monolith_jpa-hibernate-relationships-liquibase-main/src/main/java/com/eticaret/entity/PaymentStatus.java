package com.eticaret.entity;

/**
 * PAYMENT STATUS — JAVA 21 SEALED INTERFACE
 * ===========================================
 * Ödeme durumunu temsil eder.
 * OrderStatus ile aynı yapı — sealed interface gücünü gösterir.
 *
 * OrderStatus ile fark:
 *   OrderStatus: Siparişin fiziksel durumu (kargo, teslimat)
 *   PaymentStatus: Ödemenin finansal durumu (bekliyor, tamamlandı, iade)
 *
 * Gerçek uygulamada ödeme sistemi (Stripe, İyzico) bu durumları gönderir.
 */
public sealed interface PaymentStatus
        permits PaymentStatus.Pending,
                PaymentStatus.Paid,
                PaymentStatus.Failed,
                PaymentStatus.Refunded,
                PaymentStatus.PartiallyRefunded {

    String code();
    String displayName();
    boolean isFinal();

    // Ödeme tamamlandı mı? (sipariş işleme alınabilir)
    default boolean isSuccessful() {
        return this instanceof Paid;
    }

    // ===== PERMITTED IMPLEMENTATIONS =====

    record Pending() implements PaymentStatus {
        public String code() { return "PENDING"; }
        public String displayName() { return "Ödeme Bekleniyor"; }
        public boolean isFinal() { return false; }
    }

    record Paid() implements PaymentStatus {
        public String code() { return "PAID"; }
        public String displayName() { return "Ödendi"; }
        public boolean isFinal() { return false; }
    }

    record Failed() implements PaymentStatus {
        public String code() { return "FAILED"; }
        public String displayName() { return "Ödeme Başarısız"; }
        public boolean isFinal() { return true; }
    }

    record Refunded() implements PaymentStatus {
        public String code() { return "REFUNDED"; }
        public String displayName() { return "İade Edildi"; }
        public boolean isFinal() { return true; }
    }

    record PartiallyRefunded() implements PaymentStatus {
        public String code() { return "PARTIALLY_REFUNDED"; }
        public String displayName() { return "Kısmi İade"; }
        public boolean isFinal() { return false; }
    }

    // Factory method — DB String → Java nesnesi
    static PaymentStatus fromCode(String code) {
        return switch (code) {
            case "PENDING"             -> new Pending();
            case "PAID"                -> new Paid();
            case "FAILED"              -> new Failed();
            case "REFUNDED"            -> new Refunded();
            case "PARTIALLY_REFUNDED"  -> new PartiallyRefunded();
            default -> throw new IllegalArgumentException("Bilinmeyen ödeme durumu: " + code);
        };
    }
}
