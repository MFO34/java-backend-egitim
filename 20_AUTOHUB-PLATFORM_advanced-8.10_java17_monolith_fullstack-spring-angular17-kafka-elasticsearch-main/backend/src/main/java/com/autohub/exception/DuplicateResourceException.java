package com.autohub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Çakışan Kayıt İstisnası
 *
 * Aynı benzersiz alanlara sahip kayıt eklenmeye çalışıldığında fırlatılır.
 * Örnek: Aynı plaka numarası, aynı kullanıcı adı vs.
 */
@ResponseStatus(HttpStatus.CONFLICT)  // HTTP 409 döner
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s zaten mevcut: %s = '%s'", resourceName, fieldName, fieldValue));
    }
}
