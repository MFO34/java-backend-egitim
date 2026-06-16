package com.banka.exception;

/**
 * Geçersiz miktar girildiğinde fırlatılan exception.
 * Örnek: negatif para yatırma, sıfır transfer gibi durumlar.
 */
public class InvalidAmountException extends RuntimeException {

    private final double invalidAmount; // Geçersiz olan miktar

    public InvalidAmountException(double invalidAmount) {
        super(String.format("Geçersiz miktar: %.2f TL — Miktar sıfırdan büyük olmalıdır.", invalidAmount));
        this.invalidAmount = invalidAmount;
    }

    public InvalidAmountException(String message) {
        super(message);
        this.invalidAmount = 0;
    }

    public double getInvalidAmount() {
        return invalidAmount;
    }
}
