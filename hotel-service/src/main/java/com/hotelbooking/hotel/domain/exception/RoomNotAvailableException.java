package com.hotelbooking.hotel.domain.exception;

public class RoomNotAvailableException extends HotelException {

	public RoomNotAvailableException(Long roomId) {
		super("Room " + roomId + " is not operationally available");
	}
}
