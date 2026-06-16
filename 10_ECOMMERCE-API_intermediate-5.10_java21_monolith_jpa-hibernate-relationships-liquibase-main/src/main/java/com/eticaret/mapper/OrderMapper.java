package com.eticaret.mapper;

import com.eticaret.dto.response.OrderItemResponse;
import com.eticaret.dto.response.OrderResponse;
import com.eticaret.entity.Order;
import com.eticaret.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "userId",               source = "user.id")
    @Mapping(target = "userFullName",         source = "user", qualifiedByName = "userFullName")
    @Mapping(target = "statusCode",           expression = "java(order.getStatus().code())")
    @Mapping(target = "statusDisplayName",    expression = "java(order.getStatus().displayName())")
    @Mapping(target = "paymentStatusCode",    expression = "java(order.getPaymentStatus().code())")
    @Mapping(target = "paymentStatusDisplayName", expression = "java(order.getPaymentStatus().displayName())")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId",   source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSlug", source = "product.slug")
    OrderItemResponse toItemResponse(OrderItem item);

    @org.mapstruct.Named("userFullName")
    default String userFullName(com.eticaret.entity.User user) {
        if (user == null) return null;
        return user.getFirstName() + " " + user.getLastName();
    }
}
