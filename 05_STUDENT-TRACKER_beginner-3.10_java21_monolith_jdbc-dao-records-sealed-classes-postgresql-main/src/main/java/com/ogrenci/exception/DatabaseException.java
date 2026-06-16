package com.ogrenci.exception;

/**
 * Veritabanı işlemlerinde oluşan hataları saran genel exception.
 *
 * NEDEN SARARIZ (wrap)?
 * SQLException checked exception'dır — her yerde try/catch yazmak zorunda kalırız.
 * RuntimeException'a sararsak bu zorunluluk ortadan kalkar ve kod temiz kalır.
 */
public class DatabaseException extends RuntimeException {

    // Orijinal SQL hata kodu (debug için faydalı)
    private final int sqlErrorCode;

    public DatabaseException(String message, Throwable cause) {
        super(message, cause); // Üst sınıfa mesajı ve orijinal hatayı ilet
        this.sqlErrorCode = 0;
    }

    public DatabaseException(String message, Throwable cause, int sqlErrorCode) {
        super(message, cause);
        this.sqlErrorCode = sqlErrorCode;
    }

    public DatabaseException(String message) {
        super(message);
        this.sqlErrorCode = 0;
    }

    public int getSqlErrorCode() { return sqlErrorCode; }
}
