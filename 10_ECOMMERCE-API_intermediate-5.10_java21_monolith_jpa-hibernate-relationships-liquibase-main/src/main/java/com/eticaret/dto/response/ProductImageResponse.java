package com.eticaret.dto.response;

public record ProductImageResponse(
    Long id,
    String imageUrl,
    Boolean isPrimary,
    Integer displayOrder,
    String altText
) {}
