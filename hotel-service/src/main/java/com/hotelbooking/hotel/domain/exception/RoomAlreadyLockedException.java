package com.hotelbooking.hotel.domain.exception;

public class RoomAlreadyLockedException extends HotelException {

	public RoomAlreadyLockedException(Long roomId) {
		super("Room " + roomId + " is already locked for the requested date range");
	}
}
