package com.eticaret.exception;

/**
 * Stok yetersizliği için özel exception.
 * Optimistic Lock çakışması veya stok sıfırlanması durumunda fırlatılır.
 */
public class OutOfStockException extends BusinessException {

    private final String productName;
    private final int requested;
    private final int available;

    public OutOfStockException(String productName, int requested, int available) {
        super(
            "Yetersiz stok: '" + productName + "' — istenen: " + requested + ", mevcut: " + available,
            "OUT_OF_STOCK"
        );
        this.productName = productName;
        this.requested   = requested;
        this.available   = available;
    }

    public String getProductName() { return productName; }
    public int getRequested()      { return requested; }
    public int getAvailable()      { return available; }
}
