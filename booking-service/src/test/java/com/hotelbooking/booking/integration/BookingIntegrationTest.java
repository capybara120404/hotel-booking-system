package com.hotelbooking.booking.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.hotelbooking.booking.application.dto.request.AuthRequest;
import com.hotelbooking.booking.application.dto.request.CreateBookingRequest;
import com.hotelbooking.booking.application.dto.response.AuthResponse;
import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.application.dto.response.RoomResponse;
import com.hotelbooking.booking.application.port.HotelServiceClient;
import com.hotelbooking.booking.domain.entity.Booking;
import com.hotelbooking.booking.domain.entity.BookingStatus;
import com.hotelbooking.booking.domain.entity.Role;
import com.hotelbooking.booking.domain.entity.User;
import com.hotelbooking.booking.infrastructure.persistence.BookingRepository;
import com.hotelbooking.booking.infrastructure.persistence.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Booking Integration Tests")
class BookingIntegrationTest {
	@Autowired
	private TestRestTemplate restTemplate;
	@Autowired
	private BookingRepository bookingRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private HotelServiceClient hotelServiceClient;

	private String userToken;

	@BeforeEach
	void setUp() {
		bookingRepository.deleteAll();
		userRepository.deleteAll();
		userRepository.save(new User("user", passwordEncoder.encode("user123"), Role.USER));
		userRepository.save(new User("admin", passwordEncoder.encode("admin123"), Role.ADMIN));

		ResponseEntity<AuthResponse> userAuthResponse = restTemplate.postForEntity("/user/auth",
				new AuthRequest("user", "user123"), AuthResponse.class);
		assertThat(userAuthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		userToken = userAuthResponse.getBody().token();
	}

	@Test
	@DisplayName("should create booking successfully with mocked hotel service")
	void shouldCreateBookingSuccessfully() {
		Long roomId = 100L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);

		doNothing().when(hotelServiceClient).confirmAvailability(eq(roomId), eq(startDate), eq(endDate), anyString(),
				anyString());

		CreateBookingRequest request = new CreateBookingRequest(roomId, startDate, endDate, false);
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(userToken);
		headers.set("Idempotency-Key", "test-key-1");
		HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);

		ResponseEntity<BookingResponse> response = restTemplate.exchange("/api/bookings", HttpMethod.POST, entity,
				BookingResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(BookingStatus.CONFIRMED);
		assertThat(response.getBody().roomId()).isEqualTo(roomId);
		assertThat(bookingRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("should cancel booking and release lock when hotel service fails")
	void shouldCompensateWhenHotelServiceFails() {
		Long roomId = 100L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);

		doThrow(new RuntimeException("Hotel service unavailable")).when(hotelServiceClient)
				.confirmAvailability(eq(roomId), eq(startDate), eq(endDate), anyString(), anyString());
		doNothing().when(hotelServiceClient).releaseLock(eq(roomId), anyString(), anyString());

		CreateBookingRequest request = new CreateBookingRequest(roomId, startDate, endDate, false);
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(userToken);
		headers.set("Idempotency-Key", "fail-key");
		HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);

		ResponseEntity<String> response = restTemplate.exchange("/api/bookings", HttpMethod.POST, entity, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(bookingRepository.count()).isEqualTo(1);

		Optional<Booking> savedBooking = bookingRepository.findAll().stream().findFirst();
		assertThat(savedBooking).isPresent();
		assertThat(savedBooking.get().getStatus()).isEqualTo(BookingStatus.CANCELLED);
	}

	@Test
	@DisplayName("should handle concurrent bookings with idempotency correctly")
	void shouldHandleConcurrentBookingsWithIdempotency() throws Exception {
		Long roomId = 100L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		String idempotencyKey = "concurrent-key";
		int threadCount = 5;

		doNothing().when(hotelServiceClient).confirmAvailability(eq(roomId), eq(startDate), eq(endDate), anyString(),
				anyString());

		CreateBookingRequest request = new CreateBookingRequest(roomId, startDate, endDate, false);
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(userToken);
		headers.set("Idempotency-Key", idempotencyKey);
		HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger conflictCount = new AtomicInteger(0);
		AtomicInteger otherCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					ResponseEntity<String> response = restTemplate.exchange("/api/bookings", HttpMethod.POST, entity,
							String.class);
					if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
						successCount.incrementAndGet();
					} else if (response.getStatusCode() == HttpStatus.CONFLICT) {
						conflictCount.incrementAndGet();
					} else {
						otherCount.incrementAndGet();
					}
				} catch (Exception e) {
					otherCount.incrementAndGet();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(finished).isTrue();
		assertThat(successCount.get() + conflictCount.get()).isEqualTo(threadCount);
		assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
		assertThat(otherCount.get()).isEqualTo(0);
		assertThat(bookingRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("should auto-select next room when first is unavailable")
	void shouldAutoSelectNextRoomWhenFirstIsUnavailable() {
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);

		RoomResponse room1 = new RoomResponse(100L, 1L, "101", true, 0L);
		RoomResponse room2 = new RoomResponse(101L, 1L, "102", true, 1L);

		doThrow(new RuntimeException("Room locked")).when(hotelServiceClient).confirmAvailability(eq(100L),
				eq(startDate), eq(endDate), anyString(), anyString());
		doNothing().when(hotelServiceClient).confirmAvailability(eq(101L), eq(startDate), eq(endDate), anyString(),
				anyString());
		doNothing().when(hotelServiceClient).releaseLock(eq(100L), anyString(), anyString());
		when(hotelServiceClient.getRecommendedRooms(eq(startDate), eq(endDate), anyString()))
				.thenReturn(List.of(room1, room2));

		CreateBookingRequest request = new CreateBookingRequest(null, startDate, endDate, true);
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(userToken);
		HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);

		ResponseEntity<BookingResponse> response = restTemplate.exchange("/api/bookings", HttpMethod.POST, entity,
				BookingResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().roomId()).isEqualTo(101L);
		assertThat(response.getBody().status()).isEqualTo(BookingStatus.CONFIRMED);
		assertThat(bookingRepository.count()).isEqualTo(2);
		assertThat(bookingRepository.findByStatus(BookingStatus.CONFIRMED)).hasSize(1);
		assertThat(bookingRepository.findByStatus(BookingStatus.CANCELLED)).hasSize(1);
	}
}
