package com.banka.exception;

/**
 * CUSTOM EXCEPTION (Özel İstisna) KAVRAMI:
 *
 * Java'nın yerleşik exception'ları (NullPointerException, IOException vb.)
 * bazen iş mantığımızı yeterince açıklamaz.
 * Kendi exception sınıflarımızı yazarak:
 *   - Hata mesajlarını özelleştirebiliriz
 *   - Ek bilgi (bakiye miktarı gibi) taşıyabiliriz
 *   - Kodun okunabilirliğini artırabiliriz
 *
 * Exception hiyerarşisi:
 *   Throwable
 *     ├── Error         (sistem hataları — yakalanmaz)
 *     └── Exception     (uygulama hataları — yakalanabilir)
 *           ├── RuntimeException  (unchecked — try/catch zorunlu değil)
 *           └── Diğerleri         (checked — try/catch ZORUNLU)
 *
 * RuntimeException extend ettiğimiz için "unchecked" exception — zorunlu try/catch yok.
 */
public class InsufficientFundsException extends RuntimeException {

    // Yetersiz olan bakiye miktarını tutan alan
    private final double availableBalance; // Mevcut bakiye
    private final double requestedAmount;  // İstenen miktar

    // Constructor 1: Sadece mesajla
    public InsufficientFundsException(String message) {
        super(message); // Üst sınıf (RuntimeException) constructor'ını çağır
        this.availableBalance = 0;
        this.requestedAmount = 0;
    }

    // Constructor 2: Mesaj + bakiye bilgisiyle (method overloading örneği!)
    public InsufficientFundsException(double availableBalance, double requestedAmount) {
        // super() — üst sınıfın constructor'ını çağırma (inheritance)
        super(String.format(
            "Yetersiz bakiye! Mevcut: %.2f TL, İstenen: %.2f TL, Eksik: %.2f TL",
            availableBalance,
            requestedAmount,
            requestedAmount - availableBalance
        ));
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    // Getter metodları
    public double getAvailableBalance() {
        return availableBalance;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    // Eksik miktarı hesaplayan yardımcı metod
    public double getShortfall() {
        return requestedAmount - availableBalance;
    }
}
