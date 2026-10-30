package com.hotelbooking.hotel.application.mapper;

import com.hotelbooking.hotel.application.dto.response.HotelResponse;
import com.hotelbooking.hotel.domain.entity.Hotel;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HotelMapper {
	HotelResponse toResponse(Hotel hotel);
}
