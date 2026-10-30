package com.hotelbooking.hotel.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHotelRequest(
		@NotBlank(message = "Hotel name is required") @Size(max = 200, message = "Hotel name must not exceed 200 characters") String name,

		@NotBlank(message = "Hotel address is required") @Size(max = 500, message = "Hotel address must not exceed 500 characters") String address) {
}
