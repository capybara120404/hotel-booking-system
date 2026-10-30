package com.hotelbooking.booking.infrastructure.client;

import java.time.LocalDate;
import java.util.List;

import com.hotelbooking.booking.application.dto.response.RoomResponse;

public interface HotelServiceClient {
	List<RoomResponse> getRecommendedRooms(LocalDate startDate, LocalDate endDate, String token);
	void confirmAvailability(Long roomId, LocalDate startDate, LocalDate endDate, String requestId, String token);
	void releaseLock(Long roomId, String requestId, String token);
}
