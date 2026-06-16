package com.eticaret.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 200) String slug,
    String description,
    Long parentId         // null → ana kategori, dolu → alt kategori
) {}
