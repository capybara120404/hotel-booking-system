package com.hotelbooking.booking.application.mapper;

import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.domain.entity.Booking;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {

	BookingResponse toResponse(Booking booking);
}
