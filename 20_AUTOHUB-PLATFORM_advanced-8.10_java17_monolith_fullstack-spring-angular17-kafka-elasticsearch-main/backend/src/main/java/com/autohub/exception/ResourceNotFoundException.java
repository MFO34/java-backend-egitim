package com.autohub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Kaynak Bulunamadı İstisnası
 *
 * OOP Prensibi - KALITIM (Inheritance):
 * RuntimeException'dan miras alır - checked exception değil
 * Servis metotlarında throws bildirimi zorunlu değildir
 *
 * @ResponseStatus: Bu exception fırlatıldığında 404 döner (Controller seviyesinde)
 */
@ResponseStatus(HttpStatus.NOT_FOUND)  // HTTP 404 döner
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Mesajlı constructor
     *
     * @param message Hata açıklaması
     */
    public ResourceNotFoundException(String message) {
        // Parent RuntimeException'ın mesajlı constructor'ını çağır
        super(message);
    }

    /**
     * Kaynak adı ve değeri ile dinamik mesaj oluşturur
     *
     * @param resourceName Kaynak tipi adı (Vehicle, Customer vs.)
     * @param fieldName Alan adı (id, licensePlate vs.)
     * @param fieldValue Alan değeri
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // Dinamik hata mesajı oluştur - daha açıklayıcı
        super(String.format("%s bulunamadı: %s = '%s'", resourceName, fieldName, fieldValue));
    }

    /**
     * Mesaj ve neden ile constructor
     *
     * @param message Hata açıklaması
     * @param cause Hatanın sebebi olan exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
