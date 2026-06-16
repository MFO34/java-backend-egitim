package com.eticaret.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER — Merkezi Hata Yönetimi
 * ===================================================
 * @RestControllerAdvice:
 *   Tüm @RestController'lardaki exception'ları yakalar.
 *   try/catch bloğu her controller'da tekrar yazmak yerine tek yer.
 *
 * Pattern Matching for switch (Java 21):
 *   exception instanceof ResourceNotFoundException e → exhaustive switch
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 404 Not Found — Kaynak bulunamadı
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Kaynak bulunamadı: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage()
            ));
    }

    /**
     * 400/409 — İş kuralı ihlali
     * Pattern Matching for switch: errorCode'a göre HTTP status seç
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("İş kuralı ihlali [{}]: {}", ex.getErrorCode(), ex.getMessage());

        // Java 21 Pattern Matching for switch
        HttpStatus status = switch (ex.getErrorCode()) {
            case "DUPLICATE_EMAIL",
                 "DUPLICATE_SLUG",
                 "ALREADY_REVIEWED"   -> HttpStatus.CONFLICT;
            case "OUT_OF_STOCK",
                 "CART_EMPTY",
                 "INVALID_STATUS_TRANSITION",
                 "INVALID_QUANTITY"   -> HttpStatus.BAD_REQUEST;
            case "ORDER_CANCELLED",
                 "ORDER_ALREADY_PAID" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default                   -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status)
            .body(new ErrorResponse(status.value(), ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 409 Conflict — Optimistic Locking çakışması
     * İki kullanıcı aynı anda stok güncellemesi yaparsa.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock çakışması: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "CONCURRENT_MODIFICATION",
                "Kayıt başka bir işlem tarafından güncellendi. Lütfen tekrar deneyin."
            ));
    }

    /**
     * 400 Bad Request — @Valid doğrulama hatası
     * Hangi alanların neden hatalı olduğunu döndürür.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        var errors = new HashMap<String, String>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            var fieldName = ((FieldError) error).getField();
            var message   = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        log.warn("Doğrulama hatası: {}", errors);

        return ResponseEntity.badRequest()
            .body(new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Giriş doğrulama başarısız",
                errors
            ));
    }

    /**
     * 500 Internal Server Error — Beklenmeyen hatalar
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Beklenmeyen hata: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin."
            ));
    }

    // ===== ERROR RESPONSE RECORD'LARI =====

    public record ErrorResponse(
        int status,
        String errorCode,
        String message,
        LocalDateTime timestamp
    ) {
        // Compact constructor: timestamp otomatik ata
        public ErrorResponse(int status, String errorCode, String message) {
            this(status, errorCode, message, LocalDateTime.now());
        }
    }

    public record ValidationErrorResponse(
        int status,
        String errorCode,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
    ) {
        public ValidationErrorResponse(int status, String errorCode,
                                       String message, Map<String, String> fieldErrors) {
            this(status, errorCode, message, fieldErrors, LocalDateTime.now());
        }
    }
}
