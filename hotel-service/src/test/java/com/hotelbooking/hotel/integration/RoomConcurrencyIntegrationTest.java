package com.hotelbooking.hotel.integration;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hotelbooking.hotel.application.dto.request.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.application.service.RoomService;
import com.hotelbooking.hotel.domain.entity.Hotel;
import com.hotelbooking.hotel.domain.entity.Room;
import com.hotelbooking.hotel.domain.exception.RoomAlreadyLockedException;
import com.hotelbooking.hotel.infrastructure.persistence.HotelRepository;
import com.hotelbooking.hotel.infrastructure.persistence.RoomRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Room Concurrency Integration Tests")
class RoomConcurrencyIntegrationTest {

	@Autowired
	private RoomService roomService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private HotelRepository hotelRepository;

	private Long roomId;

	@BeforeEach
	void setUp() {
		roomRepository.deleteAll();
		hotelRepository.deleteAll();
		Hotel hotel = hotelRepository.save(new Hotel("Test Hotel", "Test Address"));
		Room room = roomRepository.save(new Room(hotel.getId(), "CONCURRENT-101"));
		this.roomId = room.getId();
	}

	@Test
	@DisplayName("should allow only one concurrent booking for the same room and dates")
	void shouldAllowOnlyOneConcurrentBooking() throws Exception {
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		int threadCount = 5;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger conflictCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			final String requestId = "req-" + i;
			executor.submit(() -> {
				try {
					startLatch.await();
					ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest(startDate, endDate, requestId);
					roomService.confirmAvailability(roomId, request);
					successCount.incrementAndGet();
				} catch (RoomAlreadyLockedException e) {
					conflictCount.incrementAndGet();
				} catch (Exception e) {
					errorCount.incrementAndGet();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		doneLatch.await(10, TimeUnit.SECONDS);
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
		assertThat(errorCount.get()).isEqualTo(0);
	}
}
