package com.hotelbooking.hotel.application.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.hotel.application.dto.request.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.application.dto.request.CreateRoomRequest;
import com.hotelbooking.hotel.application.dto.request.UpdateRoomRequest;
import com.hotelbooking.hotel.application.dto.response.RoomResponse;
import com.hotelbooking.hotel.application.mapper.RoomMapper;
import com.hotelbooking.hotel.domain.entity.Room;
import com.hotelbooking.hotel.domain.entity.RoomLock;
import com.hotelbooking.hotel.domain.exception.EntityNotFoundException;
import com.hotelbooking.hotel.domain.exception.RoomAlreadyLockedException;
import com.hotelbooking.hotel.domain.exception.RoomNotAvailableException;
import com.hotelbooking.hotel.infrastructure.persistence.HotelRepository;
import com.hotelbooking.hotel.infrastructure.persistence.RoomLockRepository;
import com.hotelbooking.hotel.infrastructure.persistence.RoomRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class RoomService {
	private static final Logger log = LoggerFactory.getLogger(RoomService.class);

	private final RoomRepository roomRepository;
	private final RoomLockRepository roomLockRepository;
	private final HotelRepository hotelRepository;
	private final RoomMapper roomMapper;

	public RoomService(RoomRepository roomRepository, RoomLockRepository roomLockRepository,
			HotelRepository hotelRepository, RoomMapper roomMapper) {
		this.roomRepository = roomRepository;
		this.roomLockRepository = roomLockRepository;
		this.hotelRepository = hotelRepository;
		this.roomMapper = roomMapper;
	}

	@Transactional
	public RoomResponse create(CreateRoomRequest request) {
		if (!hotelRepository.existsById(request.hotelId())) {
			throw new EntityNotFoundException("Hotel", request.hotelId());
		}
		Room room = new Room(request.hotelId(), request.number());
		return roomMapper.toResponse(roomRepository.save(room));
	}

	public Page<RoomResponse> findByHotel(Long hotelId, Pageable pageable) {
		if (!hotelRepository.existsById(hotelId)) {
			throw new EntityNotFoundException("Hotel", hotelId);
		}
		return roomRepository.findByHotelId(hotelId, pageable).map(roomMapper::toResponse);
	}

	public Page<RoomResponse> findFreeRooms(LocalDate startDate, LocalDate endDate, Pageable pageable) {
		return roomRepository.findFreeRooms(startDate, endDate, pageable).map(roomMapper::toResponse);
	}

	public Page<RoomResponse> findRecommendedRooms(LocalDate startDate, LocalDate endDate, Pageable pageable) {
		return roomRepository.findRecommendedRooms(startDate, endDate, pageable).map(roomMapper::toResponse);
	}

	@Transactional
	public RoomResponse update(Long id, UpdateRoomRequest request) {
		Room room = findRoomForUpdateOrThrow(id);
		room.update(request.number(), request.available());
		return roomMapper.toResponse(room);
	}

	@Transactional
	public void delete(Long id) {
		Room room = findRoomForUpdateOrThrow(id);
		roomRepository.delete(room);
	}

	@Transactional
	public void confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
		if (!request.startDate().isBefore(request.endDate())) {
			throw new IllegalArgumentException("Start date must be before end date");
		}
		Room room = roomRepository.findByIdForUpdate(roomId)
				.orElseThrow(() -> new EntityNotFoundException("Room", roomId));

		if (roomLockRepository.findByRequestId(request.requestId()).isPresent()) {
			log.info("Idempotent confirm-availability for requestId={}", request.requestId());
			return;
		}

		if (!room.isAvailable()) {
			throw new RoomNotAvailableException(roomId);
		}

		if (roomLockRepository.existsOverlapping(roomId, request.startDate(), request.endDate())) {
			throw new RoomAlreadyLockedException(roomId);
		}

		RoomLock lock = new RoomLock(roomId, request.startDate(), request.endDate(), request.requestId());
		roomLockRepository.save(lock);
		log.info("Held room {} for requestId={} from {} to {}", roomId, request.requestId(), request.startDate(),
				request.endDate());
	}

	@Transactional
	public void commitLock(Long roomId, String requestId) {
		RoomLock lock = roomLockRepository.findByRequestId(requestId)
				.orElseThrow(() -> new EntityNotFoundException("RoomLock", requestId));
		if (!lock.getRoomId().equals(roomId)) {
			throw new IllegalArgumentException("Lock roomId mismatch");
		}
		lock.confirm();
		roomRepository.incrementTimesBooked(roomId);
		log.info("Committed lock for room {} and requestId={}", roomId, requestId);
	}

	@Transactional
	public void releaseLock(Long roomId, String requestId) {
		Optional<RoomLock> lockOpt = roomLockRepository.findByRequestId(requestId);
		if (lockOpt.isPresent() && lockOpt.get().getRoomId().equals(roomId)) {
			roomLockRepository.delete(lockOpt.get());
			log.info("Released held lock for room {} and requestId={}", roomId, requestId);
		} else {
			log.info("No held lock found for room {} and requestId={}", roomId, requestId);
		}
	}

	private Room findRoomForUpdateOrThrow(Long id) {
		return roomRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Room", id));
	}
}
