package com.autohub.exception;

// Spring Web ve doğrulama importları
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Hata Yöneticisi - Tüm uygulama genelinde istisnaları yakalar
 *
 * OOP Prensibi - SORUMLULUK AYIRIMI:
 * Hata yönetimi bu sınıfta merkezi olarak yapılır.
 * Controller'lar try-catch yazmak zorunda kalmaz.
 *
 * OOP Prensibi - POLİMORFİZM (Polymorphism):
 * Farklı exception tipleri için farklı @ExceptionHandler metotları
 * - her hata tipi için polimorfik davranış
 *
 * @RestControllerAdvice: Tüm Controller'ları kapsayan AOP advice
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Kaynak bulunamadı hatası
     * Araç, müşteri, kullanıcı vs. bulunamadığında
     *
     * @param ex ResourceNotFoundException
     * @param request Web isteği bağlamı
     * @return 404 Not Found + hata detayları
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Kaynak bulunamadı: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Kaynak Bulunamadı")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Duplicate kayıt hatası
     * Aynı plaka, kullanıcı adı, e-posta vs. tekrar kaydedilmek istendiğinde
     *
     * @return 409 Conflict + hata detayları
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplicate kayıt: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Çakışan Kayıt")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Doğrulama hatası - @Valid anotasyonu ile tetiklenir
     * Gelen request body'deki field validation hataları için
     *
     * @return 400 Bad Request + alan bazında hata mesajları
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Doğrulama hatası: {}", ex.getMessage());

        // Alan bazında hata mesajlarını topla
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                // Her alan için hata mesajını map'e ekle
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        });

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Doğrulama Hatası",
            "İstek verileri geçersiz",
            request.getDescription(false).replace("uri=", ""),
            fieldErrors
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Hatalı kimlik bilgileri - yanlış şifre veya kullanıcı adı
     *
     * @return 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        log.warn("Hatalı kimlik bilgileri");

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Kimlik Doğrulama Hatası")
            // Güvenlik: Detaylı mesaj verme - hangisinin yanlış olduğu söylenmez
            .message("Kullanıcı adı veya şifre hatalı")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Yetkisiz erişim hatası - kullanıcı gerekli role sahip değil
     *
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Yetkisiz erişim denemesi: {}", request.getDescription(false));

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Erişim Reddedildi")
            .message("Bu işlem için yetkiniz bulunmuyor")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * İş mantığı hatası - geçersiz işlem denemesi
     *
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        log.error("İş mantığı hatası: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Geçersiz İşlem")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Beklenmeyen genel hata - yakalanmayan tüm exception'lar için
     *
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Beklenmeyen hata: ", ex);

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Sunucu Hatası")
            // Üretim ortamında detaylı hata mesajı gösterme (güvenlik)
            .message("Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
