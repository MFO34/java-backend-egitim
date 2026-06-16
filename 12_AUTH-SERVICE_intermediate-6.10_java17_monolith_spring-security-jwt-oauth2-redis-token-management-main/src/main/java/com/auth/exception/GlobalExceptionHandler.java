package com.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GLOBAL HATA İŞLEYİCİ
 * =====================
 * @RestControllerAdvice: Tüm controller'larda fırlatılan exception'ları yakalar.
 * Her exception tipi için uygun HTTP status code ve hata mesajı döner.
 *
 * Spring Security exception'ları:
 *   AuthenticationException → 401 Unauthorized (kimlik doğrulanamadı)
 *   AccessDeniedException   → 403 Forbidden (yetkisiz erişim)
 *
 * Validation exception'ları:
 *   MethodArgumentNotValidException → 400 Bad Request (field validation hatası)
 *
 * Business exception'ları:
 *   IllegalArgumentException → 400 Bad Request (geçersiz istek)
 *
 * Hata yanıt formatı:
 *   {
 *     "timestamp": "2024-01-01T12:00:00",
 *     "status": 401,
 *     "error": "Unauthorized",
 *     "message": "Geçersiz kimlik bilgileri.",
 *     "path": "/api/v1/auth/login"
 *   }
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== GÜVENLİK EXCEPTION'LARI ==========

    /**
     * 401 UNAUTHORIZED
     * Giriş yapılmamış veya token geçersiz.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex) {
        log.warn("Authentication başarısız: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "Kimlik doğrulama başarısız.", ex.getMessage());
    }

    /**
     * 403 FORBIDDEN
     * Giriş yapılmış ama yetki yok (örn: USER rolü ADMIN endpoint'e erişmeye çalışıyor).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex) {
        log.warn("Yetkisiz erişim denemesi: {}", ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok.", ex.getMessage());
    }

    // ========== VALİDASYON EXCEPTION'LARI ==========

    /**
     * 400 BAD REQUEST — Validation Hataları
     * @Valid annotation'ı başarısız olduğunda fırlatılır.
     *
     * Yanıt örneği:
     *   {
     *     "errors": {
     *       "email": "Geçerli bir e-posta adresi giriniz.",
     *       "password": "Şifre en az 8 karakter olmalıdır."
     *     }
     *   }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Field → hata mesajı map'i oluştur
        var fieldErrors = new HashMap<String, String>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        var body = new HashMap<String, Object>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("errors", fieldErrors); // Field bazlı hatalar

        log.debug("Validation hatası: {}", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ========== BUSINESS LOGIC EXCEPTION'LARI ==========

    /**
     * 400 BAD REQUEST — Geçersiz İstek
     * AuthService'de "Bu e-posta zaten kullanılıyor" gibi durumlar.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.debug("Geçersiz istek: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    /**
     * 404 NOT FOUND
     * Kaynak bulunamadığında fırlatılan exception.
     */
    @ExceptionHandler(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(
            org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
        log.debug("Kullanıcı bulunamadı: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    /**
     * 500 INTERNAL SERVER ERROR
     * Beklenmedik hatalar — production'da detayı gizle.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Beklenmedik hata: ", ex); // Stack trace logla
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
            "Sunucu hatası oluştu. Lütfen daha sonra tekrar deneyin.", null);
    }

    // ========== YARDIMCI METODLAR ==========

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message, String detail) {
        var error = new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            detail
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Standart hata yanıt yapısı.
     * Record kullandık çünkü immutable ve basit DTO.
     */
    public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String detail     // null olabilir (hassas detay production'da gizlenir)
    ) {}
}
