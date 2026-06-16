package com.eticaret.mapper;

import com.eticaret.dto.response.ReviewResponse;
import com.eticaret.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "userId",       source = "user.id")
    @Mapping(target = "userFullName", expression = "java(review.getUser().getFirstName() + \" \" + review.getUser().getLastName())")
    ReviewResponse toResponse(Review review);
}
