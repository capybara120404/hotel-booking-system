package com.hotelbooking.hotel.application.mapper;

import com.hotelbooking.hotel.application.dto.response.HotelResponse;
import com.hotelbooking.hotel.domain.entity.Hotel;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-29T12:21:55+0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.200.v20261008-1115, environment: Java 25.0.4.1 (Eclipse Adoptium)"
)
@Component
public class HotelMapperImpl implements HotelMapper {

    @Override
    public HotelResponse toResponse(Hotel hotel) {
        if ( hotel == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String address = null;

        id = hotel.getId();
        name = hotel.getName();
        address = hotel.getAddress();

        HotelResponse hotelResponse = new HotelResponse( id, name, address );

        return hotelResponse;
    }
}
