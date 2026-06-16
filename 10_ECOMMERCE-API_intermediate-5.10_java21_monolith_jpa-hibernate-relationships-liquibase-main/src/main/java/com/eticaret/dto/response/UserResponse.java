package com.eticaret.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    Boolean isActive,
    AddressResponse address,
    LocalDateTime createdAt
) {}
