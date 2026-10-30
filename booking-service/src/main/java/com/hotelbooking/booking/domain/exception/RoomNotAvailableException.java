package com.hotelbooking.booking.domain.exception;

public class RoomNotAvailableException extends BookingException {

	public RoomNotAvailableException(Long roomId) {
		super(roomId != null ? "Room " + roomId + " is not available" : "No rooms available");
	}
}
