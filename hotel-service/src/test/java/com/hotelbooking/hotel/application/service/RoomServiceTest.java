package com.hotelbooking.hotel.application.service;

import java.time.LocalDate;
import java.util.Optional;

import com.hotelbooking.hotel.application.dto.request.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.application.dto.request.CreateRoomRequest;
import com.hotelbooking.hotel.application.mapper.RoomMapper;
import com.hotelbooking.hotel.domain.entity.Room;
import com.hotelbooking.hotel.domain.entity.RoomLock;
import com.hotelbooking.hotel.domain.exception.EntityNotFoundException;
import com.hotelbooking.hotel.domain.exception.RoomAlreadyLockedException;
import com.hotelbooking.hotel.domain.exception.RoomNotAvailableException;
import com.hotelbooking.hotel.infrastructure.persistence.HotelRepository;
import com.hotelbooking.hotel.infrastructure.persistence.RoomLockRepository;
import com.hotelbooking.hotel.infrastructure.persistence.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService")
class RoomServiceTest {
	@Mock
	private RoomRepository roomRepository;
	@Mock
	private RoomLockRepository roomLockRepository;
	@Mock
	private HotelRepository hotelRepository;
	@Mock
	private RoomMapper roomMapper;
	private RoomService roomService;

	@BeforeEach
	void setUp() {
		roomService = new RoomService(roomRepository, roomLockRepository, hotelRepository, roomMapper);
	}

	@Test
	@DisplayName("should create room when hotel exists")
	void createRoom_whenHotelExists_shouldCreateRoom() {
		CreateRoomRequest request = new CreateRoomRequest(1L, "101");
		when(hotelRepository.existsById(1L)).thenReturn(true);
		Room room = new Room(1L, "101");
		when(roomRepository.save(any(Room.class))).thenReturn(room);
		roomService.create(request);
		verify(roomRepository).save(any(Room.class));
	}

	@Test
	@DisplayName("should throw exception when hotel does not exist")
	void createRoom_whenHotelDoesNotExist_shouldThrowException() {
		CreateRoomRequest request = new CreateRoomRequest(999L, "101");
		when(hotelRepository.existsById(999L)).thenReturn(false);
		assertThatThrownBy(() -> roomService.create(request)).isInstanceOf(EntityNotFoundException.class)
				.hasMessageContaining("Hotel");
	}

	@Test
	@DisplayName("should confirm availability when room is free")
	void confirmAvailability_whenRoomIsFree_shouldCreateLock() {
		Long roomId = 1L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		String requestId = "test-request-id";
		ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest(startDate, endDate, requestId);
		Room room = new Room(1L, "101");
		when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
		when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(room));
		when(roomLockRepository.existsOverlapping(roomId, startDate, endDate)).thenReturn(false);

		roomService.confirmAvailability(roomId, request);

		verify(roomLockRepository).save(any(RoomLock.class));
		verify(roomRepository, never()).incrementTimesBooked(roomId);
	}

	@Test
	@DisplayName("should commit lock and increment times booked")
	void commitLock_shouldIncrementTimesBooked() {
		Long roomId = 1L;
		String requestId = "test-request-id";
		RoomLock lock = new RoomLock(roomId, LocalDate.now(), LocalDate.now().plusDays(1), requestId);
		when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.of(lock));

		roomService.commitLock(roomId, requestId);

		verify(roomRepository).incrementTimesBooked(roomId);
	}

	@Test
	@DisplayName("should be idempotent when lock already exists with same requestId")
	void confirmAvailability_whenLockExistsWithSameRequestId_shouldBeIdempotent() {
		Long roomId = 1L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		String requestId = "existing-request-id";
		ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest(startDate, endDate, requestId);
		RoomLock existingLock = new RoomLock(roomId, startDate, endDate, requestId);
		when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(new Room(1L, "101")));
		when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.of(existingLock));

		roomService.confirmAvailability(roomId, request);

		verify(roomLockRepository, never()).save(any(RoomLock.class));
		verify(roomRepository, never()).incrementTimesBooked(roomId);
	}

	@Test
	@DisplayName("should throw exception when room is already locked for overlapping dates")
	void confirmAvailability_whenRoomAlreadyLocked_shouldThrowException() {
		Long roomId = 1L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		String requestId = "new-request-id";
		ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest(startDate, endDate, requestId);
		Room room = new Room(1L, "101");
		when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
		when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(room));
		when(roomLockRepository.existsOverlapping(roomId, startDate, endDate)).thenReturn(true);

		assertThatThrownBy(() -> roomService.confirmAvailability(roomId, request))
				.isInstanceOf(RoomAlreadyLockedException.class).hasMessageContaining("already locked");
	}

	@Test
	@DisplayName("should throw exception when room is not operationally available")
	void confirmAvailability_whenRoomNotAvailable_shouldThrowException() {
		Long roomId = 1L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		String requestId = "test-request-id";
		ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest(startDate, endDate, requestId);
		Room unavailableRoom = new Room(1L, "101");
		unavailableRoom.update("101", false);
		when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
		when(roomRepository.findByIdForUpdate(roomId)).thenReturn(Optional.of(unavailableRoom));

		assertThatThrownBy(() -> roomService.confirmAvailability(roomId, request))
				.isInstanceOf(RoomNotAvailableException.class).hasMessageContaining("not operationally available");
	}

	@Test
	@DisplayName("should release lock when exists")
	void releaseLock_whenLockExists_shouldDeleteLock() {
		Long roomId = 1L;
		String requestId = "test-request-id";
		RoomLock lock = new RoomLock(roomId, LocalDate.now(), LocalDate.now().plusDays(1), requestId);
		when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.of(lock));

		roomService.releaseLock(roomId, requestId);

		verify(roomLockRepository).delete(lock);
		verify(roomRepository, never()).decrementTimesBooked(roomId);
	}

	@Test
	@DisplayName("should handle release when lock does not exist")
	void releaseLock_whenLockDoesNotExist_shouldHandleGracefully() {
		Long roomId = 1L;
		String requestId = "non-existent-request-id";
		when(roomLockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

		roomService.releaseLock(roomId, requestId);

		verify(roomLockRepository, never()).delete(any(RoomLock.class));
		verify(roomRepository, never()).decrementTimesBooked(roomId);
	}
}
