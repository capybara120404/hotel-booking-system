package com.hotelbooking.hotel.application.mapper;

import com.hotelbooking.hotel.application.dto.response.RoomResponse;
import com.hotelbooking.hotel.domain.entity.Room;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-29T18:38:06+0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.200.v20261008-1115, environment: Java 25.0.4.1 (Eclipse Adoptium)"
)
@Component
public class RoomMapperImpl implements RoomMapper {

    @Override
    public RoomResponse toResponse(Room room) {
        if ( room == null ) {
            return null;
        }

        Long id = null;
        Long hotelId = null;
        String number = null;
        boolean available = false;
        long timesBooked = 0L;

        id = room.getId();
        hotelId = room.getHotelId();
        number = room.getNumber();
        available = room.isAvailable();
        timesBooked = room.getTimesBooked();

        RoomResponse roomResponse = new RoomResponse( id, hotelId, number, available, timesBooked );

        return roomResponse;
    }
}
