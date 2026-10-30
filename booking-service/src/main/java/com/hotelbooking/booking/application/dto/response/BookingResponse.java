package com.hotelbooking.booking.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hotelbooking.booking.domain.entity.BookingStatus;

public record BookingResponse(Long id, Long userId, Long roomId, LocalDate startDate, LocalDate endDate,
		BookingStatus status, LocalDateTime createdAt) {
}
