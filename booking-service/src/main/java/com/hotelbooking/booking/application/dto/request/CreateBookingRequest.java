package com.hotelbooking.booking.application.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(Long roomId,
		@NotNull(message = "Start date is required") @Future(message = "Start date must be in the future") LocalDate startDate,
		@NotNull(message = "End date is required") @Future(message = "End date must be in the future") LocalDate endDate,
		boolean autoSelect) {
}
