package com.eticaret.mapper;

import com.eticaret.dto.request.CreateCategoryRequest;
import com.eticaret.dto.response.CategoryResponse;
import com.eticaret.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "parent",   ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "isActive",     constant = "true")
    @Mapping(target = "displayOrder", constant = "0")
    Category toEntity(CreateCategoryRequest request);

    @Mapping(target = "parentId",    source = "parent.id")
    @Mapping(target = "parentName",  source = "parent.name")
    @Mapping(target = "productCount", ignore = true)
    CategoryResponse toResponse(Category category);
}
