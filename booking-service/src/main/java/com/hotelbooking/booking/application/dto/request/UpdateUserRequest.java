package com.hotelbooking.booking.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@NotBlank(message = "Password is required") @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters") String password,

		@NotBlank(message = "Role is required") String role) {
}
