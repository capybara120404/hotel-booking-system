package com.hotelbooking.booking.domain.exception;

public class EntityNotFoundException extends BookingException {

	public EntityNotFoundException(String entityName, Object id) {
		super(entityName + " with id '" + id + "' not found");
	}
}
