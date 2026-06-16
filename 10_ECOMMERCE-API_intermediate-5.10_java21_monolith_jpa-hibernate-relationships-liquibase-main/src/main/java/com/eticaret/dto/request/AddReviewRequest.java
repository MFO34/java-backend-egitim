package com.eticaret.dto.request;

import jakarta.validation.constraints.*;

public record AddReviewRequest(
    @NotNull Long userId,
    @NotNull Long productId,
    @NotNull @Min(1) @Max(5) Integer rating,
    @Size(max = 200) String title,
    @Size(max = 5000) String content
) {}
