package com.hotelbooking.hotel.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
		@NotBlank(message = "Room number is required") @Size(max = 50, message = "Room number must not exceed 50 characters") String number,

		boolean available) {
}
