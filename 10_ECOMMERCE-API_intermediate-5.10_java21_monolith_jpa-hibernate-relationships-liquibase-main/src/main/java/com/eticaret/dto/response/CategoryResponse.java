package com.eticaret.dto.response;

import java.util.List;

public record CategoryResponse(
    Long id,
    String name,
    String slug,
    String description,
    Long parentId,
    String parentName,
    List<CategoryResponse> children,  // Hiyerarşik yapı için özyinelemeli
    Integer productCount
) {}
