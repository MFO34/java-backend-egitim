package com.eticaret.exception;

/**
 * İş kuralı ihlalleri için kullanılan exception.
 * errorCode: Frontend'in hangi hatayı göstereceğini belirler.
 */
public class BusinessException extends RuntimeException {
    private final String errorCode;

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
