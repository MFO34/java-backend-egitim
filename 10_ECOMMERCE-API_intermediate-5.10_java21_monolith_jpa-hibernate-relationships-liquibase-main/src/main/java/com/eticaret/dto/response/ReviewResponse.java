package com.eticaret.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long userId,
    String userFullName,
    Integer rating,
    String title,
    String content,
    Boolean isApproved,
    LocalDateTime createdAt
) {}
