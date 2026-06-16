package com.eticaret.mapper;

import com.eticaret.dto.response.CartItemResponse;
import com.eticaret.dto.response.CartResponse;
import com.eticaret.entity.Cart;
import com.eticaret.entity.CartItem;
import com.eticaret.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "userId",         source = "user.id")
    @Mapping(target = "totalAmount",    expression = "java(cart.getTotalAmount())")
    @Mapping(target = "totalItemCount", expression = "java(cart.getTotalItemCount())")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "productId",      source = "product.id")
    @Mapping(target = "productName",    source = "product.name")
    @Mapping(target = "productSlug",    source = "product.slug")
    @Mapping(target = "unitPrice",      source = "product.price")
    @Mapping(target = "primaryImageUrl",source = "product.images", qualifiedByName = "primaryUrl")
    @Mapping(target = "inStock",        expression = "java(item.getProduct().isInStock())")
    CartItemResponse toItemResponse(CartItem item);

    @Named("primaryUrl")
    default String primaryUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
            .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
            .findFirst().orElse(images.get(0)).getImageUrl();
    }
}
