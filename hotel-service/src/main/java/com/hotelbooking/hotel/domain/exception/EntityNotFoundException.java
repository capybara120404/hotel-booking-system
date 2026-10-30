package com.hotelbooking.hotel.domain.exception;

public class EntityNotFoundException extends HotelException {

	public EntityNotFoundException(String entityName, Object id) {
		super(entityName + " with id '" + id + "' not found");
	}
}
