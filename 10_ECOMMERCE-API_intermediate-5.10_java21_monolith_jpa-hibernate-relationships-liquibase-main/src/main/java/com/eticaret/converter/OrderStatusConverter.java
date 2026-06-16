package com.eticaret.converter;

import com.eticaret.entity.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA ATTRİBUTE CONVERTER — OrderStatus ↔ String
 * =================================================
 * @Converter(autoApply = true):
 *   Tüm entity'lerdeki OrderStatus tipli alanları otomatik dönüştürür.
 *   @Convert(converter=...) yazmaya gerek kalmaz.
 *
 * Neden Converter gerekli?
 *   OrderStatus bir Java interface → JPA bilmez nasıl saklayacağını.
 *   Converter ile: OrderStatus → "PENDING" (DB) ve "PENDING" → OrderStatus (Java)
 *
 * AttributeConverter<X, Y>:
 *   X: Java tipi (OrderStatus)
 *   Y: DB sütun tipi (String → VARCHAR)
 */
@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    /**
     * Java → DB: Entity kaydedilirken çağrılır.
     * OrderStatus.Pending() → "PENDING"
     */
    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        if (status == null) return null;           // null güvenliği
        return status.code();                      // "PENDING", "SHIPPED" vb.
    }

    /**
     * DB → Java: Entity yüklenirken çağrılır.
     * "SHIPPED" → new OrderStatus.Shipped()
     */
    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;  // null güvenliği
        return OrderStatus.fromCode(dbData);                  // Factory method
    }
}
