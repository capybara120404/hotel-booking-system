package com.hotelbooking.hotel.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.hotel.application.dto.request.CreateHotelRequest;
import com.hotelbooking.hotel.application.dto.request.UpdateHotelRequest;
import com.hotelbooking.hotel.application.dto.response.HotelResponse;
import com.hotelbooking.hotel.application.dto.response.OccupancyStatsDto;
import com.hotelbooking.hotel.application.mapper.HotelMapper;
import com.hotelbooking.hotel.domain.entity.Hotel;
import com.hotelbooking.hotel.domain.exception.EntityNotFoundException;
import com.hotelbooking.hotel.infrastructure.persistence.HotelRepository;
import com.hotelbooking.hotel.infrastructure.persistence.RoomRepository;

@Service
@Transactional(readOnly = true)
public class HotelService {

	private final HotelRepository hotelRepository;
	private final RoomRepository roomRepository;
	private final HotelMapper hotelMapper;

	public HotelService(HotelRepository hotelRepository, RoomRepository roomRepository, HotelMapper hotelMapper) {
		this.hotelRepository = hotelRepository;
		this.roomRepository = roomRepository;
		this.hotelMapper = hotelMapper;
	}

	@Transactional
	public HotelResponse create(CreateHotelRequest request) {
		Hotel hotel = new Hotel(request.name(), request.address());
		return hotelMapper.toResponse(hotelRepository.save(hotel));
	}

	public Page<HotelResponse> findAll(String nameFilter, Pageable pageable) {
		if (nameFilter != null && !nameFilter.isBlank()) {
			return hotelRepository.findByNameContainingIgnoreCase(nameFilter, pageable).map(hotelMapper::toResponse);
		}
		return hotelRepository.findAll(pageable).map(hotelMapper::toResponse);
	}

	public HotelResponse findById(Long id) {
		return hotelMapper.toResponse(findHotelOrThrow(id));
	}

	@Transactional
	public HotelResponse update(Long id, UpdateHotelRequest request) {
		Hotel hotel = findHotelOrThrow(id);
		hotel.update(request.name(), request.address());
		return hotelMapper.toResponse(hotel);
	}

	@Transactional
	public void delete(Long id) {
		Hotel hotel = findHotelOrThrow(id);
		if (roomRepository.existsByHotelId(id)) {
			throw new IllegalArgumentException("Cannot delete hotel with existing rooms");
		}
		hotelRepository.delete(hotel);
	}

	public OccupancyStatsDto getOccupancyStats() {
		List<Object[]> roomStats = roomRepository.findStatsByHotel();
		long totalRooms = 0;
		long totalBookings = 0;
		List<OccupancyStatsDto.HotelStatsDto> hotelStats = new ArrayList<>();

		for (Object[] row : roomStats) {
			Long hotelId = (Long) row[0];
			String hotelName = (String) row[1];
			Long roomCount = (Long) row[2];
			Long bookings = (Long) row[3];

			totalRooms += roomCount;
			totalBookings += bookings;
			hotelStats.add(new OccupancyStatsDto.HotelStatsDto(hotelId, hotelName, roomCount, bookings));
		}

		return new OccupancyStatsDto(totalRooms, totalBookings, hotelStats);
	}

	private Hotel findHotelOrThrow(Long id) {
		return hotelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hotel", id));
	}
}
