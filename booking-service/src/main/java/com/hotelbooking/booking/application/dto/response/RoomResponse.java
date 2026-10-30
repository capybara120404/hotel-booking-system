
package com.hotelbooking.booking.application.dto.response;

public record RoomResponse(Long id, Long hotelId, String number, boolean available, long timesBooked) {
}
