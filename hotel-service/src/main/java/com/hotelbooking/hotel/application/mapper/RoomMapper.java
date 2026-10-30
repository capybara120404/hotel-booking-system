package com.hotelbooking.hotel.application.mapper;

import com.hotelbooking.hotel.application.dto.response.RoomResponse;
import com.hotelbooking.hotel.domain.entity.Room;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {
	RoomResponse toResponse(Room room);
}
