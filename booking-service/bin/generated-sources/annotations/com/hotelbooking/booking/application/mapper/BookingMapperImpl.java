package com.hotelbooking.booking.application.mapper;

import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.domain.entity.Booking;
import com.hotelbooking.booking.domain.entity.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-30T15:21:40+0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.200.v20261008-1115, environment: Java 25.0.4.1 (Eclipse Adoptium)"
)
@Component
public class BookingMapperImpl implements BookingMapper {

    @Override
    public BookingResponse toResponse(Booking booking) {
        if ( booking == null ) {
            return null;
        }

        Long id = null;
        Long userId = null;
        Long roomId = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        BookingStatus status = null;
        LocalDateTime createdAt = null;

        id = booking.getId();
        userId = booking.getUserId();
        roomId = booking.getRoomId();
        startDate = booking.getStartDate();
        endDate = booking.getEndDate();
        status = booking.getStatus();
        createdAt = booking.getCreatedAt();

        BookingResponse bookingResponse = new BookingResponse( id, userId, roomId, startDate, endDate, status, createdAt );

        return bookingResponse;
    }
}
