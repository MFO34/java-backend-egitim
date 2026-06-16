package com.eticaret.mapper;

import com.eticaret.dto.request.CreateUserRequest;
import com.eticaret.dto.response.AddressResponse;
import com.eticaret.dto.response.UserResponse;
import com.eticaret.entity.Address;
import com.eticaret.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * USER MAPPER
 * unmappedTargetPolicy = IGNORE:
 *   BaseEntity alanları (id, createdAt, version vb.) Builder'da yok.
 *   MapStruct bunları ignore eder — JPA/Spring otomatik doldurur.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "orders",  ignore = true)
    @Mapping(target = "cart",    ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);

    AddressResponse toAddressResponse(Address address);
}
