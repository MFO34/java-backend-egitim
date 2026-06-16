package com.eticaret.mapper;

import com.eticaret.dto.request.CreateProductRequest;
import com.eticaret.dto.response.*;
import com.eticaret.entity.Product;
import com.eticaret.entity.ProductImage;
import com.eticaret.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * PRODUCT MAPPER
 * unmappedTargetPolicy = IGNORE: BaseEntity alanları Builder'da yok → ignore.
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CategoryMapper.class})
public interface ProductMapper {

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images",   ignore = true)
    @Mapping(target = "tags",     ignore = true)
    @Mapping(target = "reviews",  ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "status",   ignore = true)
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount",   ignore = true)
    ProductResponse toResponse(Product product);

    @Mapping(target = "categoryName",    source = "category.name")
    @Mapping(target = "primaryImageUrl", source = "images", qualifiedByName = "primaryImageUrl")
    @Mapping(target = "inStock",         expression = "java(product.isInStock())")
    @Mapping(target = "averageRating",   ignore = true)
    @Mapping(target = "reviewCount",     ignore = true)
    ProductSummaryResponse toSummary(Product product);

    ProductImageResponse toImageResponse(ProductImage image);

    TagResponse toTagResponse(Tag tag);
    List<TagResponse> toTagResponses(List<Tag> tags);

    @Named("primaryImageUrl")
    default String getPrimaryImageUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
            .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
            .findFirst()
            .orElse(images.get(0))
            .getImageUrl();
    }
}
