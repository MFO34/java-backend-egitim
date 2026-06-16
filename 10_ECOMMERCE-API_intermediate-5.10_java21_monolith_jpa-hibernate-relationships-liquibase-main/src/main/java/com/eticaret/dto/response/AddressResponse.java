package com.eticaret.dto.response;

public record AddressResponse(
    Long id,
    String title,
    String street,
    String city,
    String district,
    String postalCode,
    String country
) {}
