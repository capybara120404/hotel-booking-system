package com.hotelbooking.hotel.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmAvailabilityRequest(@NotNull(message = "Start date is required") LocalDate startDate,

		@NotNull(message = "End date is required") LocalDate endDate,

		@NotBlank(message = "Request id is required") String requestId) {
}
