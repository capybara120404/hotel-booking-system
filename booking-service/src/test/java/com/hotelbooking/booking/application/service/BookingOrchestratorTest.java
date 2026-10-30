package com.hotelbooking.booking.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.hotelbooking.booking.application.dto.request.CreateBookingRequest;
import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.application.dto.response.RoomResponse;
import com.hotelbooking.booking.application.mapper.BookingMapper;
import com.hotelbooking.booking.application.port.HotelServiceClient;
import com.hotelbooking.booking.domain.entity.Booking;
import com.hotelbooking.booking.domain.entity.BookingStatus;
import com.hotelbooking.booking.domain.exception.RoomNotAvailableException;
import com.hotelbooking.booking.infrastructure.persistence.BookingRepository;
import com.hotelbooking.booking.infrastructure.security.ServiceTokenProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingOrchestrator")
class BookingOrchestratorTest {
	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private HotelServiceClient hotelServiceClient;
	@Mock
	private BookingMapper bookingMapper;
	@Mock
	private TransactionTemplate transactionTemplate;
	@Mock
	private ServiceTokenProvider serviceTokenProvider;
	private BookingOrchestrator bookingOrchestrator;
	private static final String DUMMY_TOKEN = "dummy-token";
	private static final String SERVICE_TOKEN = "service-token";

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		lenient().when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
			TransactionCallback<Object> callback = invocation.getArgument(0);
			return callback.doInTransaction(null);
		});
		lenient().when(serviceTokenProvider.getServiceToken()).thenReturn(SERVICE_TOKEN);
		bookingOrchestrator = new BookingOrchestrator(bookingRepository, hotelServiceClient, bookingMapper,
				transactionTemplate, serviceTokenProvider);
	}

	@Test
	@DisplayName("should create booking and confirm when hotel service succeeds")
	void createBooking_whenHotelServiceSucceeds_shouldConfirmBooking() {
		Long userId = 1L;
		Long roomId = 100L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		CreateBookingRequest request = new CreateBookingRequest(roomId, startDate, endDate, false);
		Booking booking = new Booking(userId, roomId, startDate, endDate, "request-id", null);
		BookingResponse response = new BookingResponse(1L, userId, roomId, startDate, endDate, BookingStatus.CONFIRMED,
				booking.getCreatedAt());
		when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
		when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
		when(bookingMapper.toResponse(any(Booking.class))).thenReturn(response);
		BookingResponse result = bookingOrchestrator.createBooking(userId, request, null, DUMMY_TOKEN);
		assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
		verify(hotelServiceClient).confirmAvailability(eq(roomId), eq(startDate), eq(endDate), anyString(),
				eq(SERVICE_TOKEN));
	}

	@Test
	@DisplayName("should cancel booking and release lock when hotel service fails")
	void createBooking_whenHotelServiceFails_shouldCancelAndCompensate() {
		Long userId = 1L;
		Long roomId = 100L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		CreateBookingRequest request = new CreateBookingRequest(roomId, startDate, endDate, false);
		Booking booking = new Booking(userId, roomId, startDate, endDate, "request-id", null);
		when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
		when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
		doThrow(new RuntimeException("Hotel service unavailable")).when(hotelServiceClient)
				.confirmAvailability(anyLong(), any(LocalDate.class), any(LocalDate.class), anyString(), anyString());
		assertThatThrownBy(() -> bookingOrchestrator.createBooking(userId, request, null, DUMMY_TOKEN))
				.isInstanceOf(RoomNotAvailableException.class);
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
		verify(hotelServiceClient).releaseLock(eq(roomId), anyString(), eq(SERVICE_TOKEN));
	}

	@Test
	@DisplayName("should auto-select room when autoSelect is true")
	void createBooking_whenAutoSelectTrue_shouldSelectRoomAutomatically() {
		Long userId = 1L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		CreateBookingRequest request = new CreateBookingRequest(null, startDate, endDate, true);
		RoomResponse recommendedRoom = new RoomResponse(200L, 1L, "201", true, 0L);
		Booking booking = new Booking(userId, 200L, startDate, endDate, "request-id", null);
		BookingResponse response = new BookingResponse(1L, userId, 200L, startDate, endDate, BookingStatus.CONFIRMED,
				booking.getCreatedAt());
		when(hotelServiceClient.getRecommendedRooms(eq(startDate), eq(endDate), anyString()))
				.thenReturn(List.of(recommendedRoom));
		when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
		when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
		when(bookingMapper.toResponse(any(Booking.class))).thenReturn(response);
		BookingResponse result = bookingOrchestrator.createBooking(userId, request, null, DUMMY_TOKEN);
		assertThat(result.roomId()).isEqualTo(200L);
		verify(hotelServiceClient).getRecommendedRooms(eq(startDate), eq(endDate), anyString());
	}

	@Test
	@DisplayName("should throw exception when no rooms available for auto-select")
	void createBooking_whenNoRoomsAvailableForAutoSelect_shouldThrowException() {
		Long userId = 1L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		CreateBookingRequest request = new CreateBookingRequest(null, startDate, endDate, true);
		when(hotelServiceClient.getRecommendedRooms(eq(startDate), eq(endDate), anyString())).thenReturn(List.of());
		assertThatThrownBy(() -> bookingOrchestrator.createBooking(userId, request, null, DUMMY_TOKEN))
				.isInstanceOf(RoomNotAvailableException.class);
	}

	@Test
	@DisplayName("should handle compensation failure gracefully")
	void createBooking_whenCompensationFails_shouldRemainInCancellingState() {
		Long userId = 1L;
		Long roomId = 100L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		CreateBookingRequest request = new CreateBookingRequest(roomId, startDate, endDate, false);
		Booking booking = new Booking(userId, roomId, startDate, endDate, "request-id", null);
		when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
		when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
		doThrow(new RuntimeException("Hotel service unavailable")).when(hotelServiceClient)
				.confirmAvailability(anyLong(), any(LocalDate.class), any(LocalDate.class), anyString(), anyString());
		doThrow(new RuntimeException("Compensation failed")).when(hotelServiceClient).releaseLock(anyLong(),
				anyString(), anyString());
		assertThatThrownBy(() -> bookingOrchestrator.createBooking(userId, request, null, DUMMY_TOKEN))
				.isInstanceOf(RoomNotAvailableException.class);
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLING);
	}
}
