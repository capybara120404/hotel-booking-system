package com.hotelbooking.booking.domain.exception;

public class HotelServiceUnavailableException extends BookingException {
	public HotelServiceUnavailableException(String message) {
		super(message);
	}

	public HotelServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
