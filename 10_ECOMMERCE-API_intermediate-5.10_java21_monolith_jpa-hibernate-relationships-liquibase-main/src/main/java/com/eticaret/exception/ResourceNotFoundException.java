package com.eticaret.exception;

/**
 * 404 Not Found için kullanılan özel exception.
 * String veya Long ID ile kullanılabilir.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(resourceName + " bulunamadı: " + resourceId);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object value) {
        super(resourceName + " bulunamadı: " + fieldName + "=" + value);
        this.resourceName = resourceName;
        this.resourceId = value;
    }

    public String getResourceName() { return resourceName; }
    public Object getResourceId()   { return resourceId; }
}
