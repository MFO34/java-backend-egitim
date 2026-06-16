package com.eticaret.converter;

import com.eticaret.entity.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA ATTRİBUTE CONVERTER — PaymentStatus ↔ String
 * OrderStatusConverter ile aynı yapı, PaymentStatus için.
 */
@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus status) {
        if (status == null) return null;
        return status.code();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        return PaymentStatus.fromCode(dbData);
    }
}
