package com.hotelbooking.booking.application.mapper;

import com.hotelbooking.booking.application.dto.response.UserResponse;
import com.hotelbooking.booking.domain.entity.User;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
	@Mapping(target = "role", expression = "java(user.getRole().name())")
	UserResponse toResponse(User user);
}
