package com.hotelbooking.hotel.domain.exception;

public abstract class HotelException extends RuntimeException {

	protected HotelException(String message) {
		super(message);
	}

	protected HotelException(String message, Throwable cause) {
		super(message, cause);
	}
}
