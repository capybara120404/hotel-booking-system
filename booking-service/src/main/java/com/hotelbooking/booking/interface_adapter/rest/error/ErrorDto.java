package com.hotelbooking.booking.interface_adapter.rest.error;

import java.time.Instant;
import java.util.List;

public record ErrorDto(Instant timestamp, int status, String error, String message, String path,
		List<FieldError> fieldErrors) {
	public record FieldError(String field, String message) {
	}

	public static ErrorDto of(int status, String error, String message, String path) {
		return new ErrorDto(Instant.now(), status, error, message, path, List.of());
	}

	public static ErrorDto of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
		return new ErrorDto(Instant.now(), status, error, message, path, fieldErrors);
	}
}
