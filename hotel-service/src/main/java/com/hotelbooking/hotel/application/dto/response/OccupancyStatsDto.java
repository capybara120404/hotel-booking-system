package com.hotelbooking.hotel.application.dto.response;

import java.util.List;

public record OccupancyStatsDto(long totalRooms, long totalBookings, List<HotelStatsDto> hotelStats) {

	public record HotelStatsDto(Long hotelId, String hotelName, long roomCount, long totalBookings) {
	}
}
