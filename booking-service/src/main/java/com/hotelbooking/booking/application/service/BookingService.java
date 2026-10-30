package com.hotelbooking.booking.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.application.mapper.BookingMapper;
import com.hotelbooking.booking.application.port.HotelServiceClient;
import com.hotelbooking.booking.domain.entity.Booking;
import com.hotelbooking.booking.domain.entity.BookingStatus;
import com.hotelbooking.booking.domain.exception.EntityNotFoundException;
import com.hotelbooking.booking.infrastructure.persistence.BookingRepository;
import com.hotelbooking.booking.infrastructure.security.ServiceTokenProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BookingService {
	private static final Logger log = LoggerFactory.getLogger(BookingService.class);

	private final BookingRepository bookingRepository;
	private final BookingMapper bookingMapper;
	private final HotelServiceClient hotelServiceClient;
	private final ServiceTokenProvider serviceTokenProvider;
	private final TransactionTemplate transactionTemplate;

	public BookingService(BookingRepository bookingRepository, BookingMapper bookingMapper,
			HotelServiceClient hotelServiceClient, ServiceTokenProvider serviceTokenProvider,
			TransactionTemplate transactionTemplate) {
		this.bookingRepository = bookingRepository;
		this.bookingMapper = bookingMapper;
		this.hotelServiceClient = hotelServiceClient;
		this.serviceTokenProvider = serviceTokenProvider;
		this.transactionTemplate = transactionTemplate;
	}

	public BookingResponse findById(Long id, Long userId) {
		Booking booking = findBookingOrThrow(id);
		if (!booking.isOwnedBy(userId)) {
			throw new AccessDeniedException("Access denied");
		}
		return bookingMapper.toResponse(booking);
	}

	public Page<BookingResponse> findByUserId(Long userId, Pageable pageable) {
		return bookingRepository.findByUserId(userId, pageable).map(bookingMapper::toResponse);
	}

	public void cancelBooking(Long id, Long userId) {
		Booking booking = findBookingOrThrow(id);
		if (!booking.isOwnedBy(userId)) {
			throw new AccessDeniedException("Access denied");
		}
		if (booking.getStatus() == BookingStatus.CANCELLED) {
			return;
		}

		BookingStatus originalStatus = booking.getStatus();
		transactionTemplate.execute(status -> {
			booking.markCancelling();
			return bookingRepository.save(booking);
		});

		boolean needsRelease = (originalStatus == BookingStatus.CONFIRMED || originalStatus == BookingStatus.PENDING);
		if (needsRelease) {
			try {
				hotelServiceClient.releaseLock(booking.getRoomId(), booking.getRequestId(),
						serviceTokenProvider.getServiceToken());
				transactionTemplate.execute(status -> {
					booking.finalizeCancellation();
					return bookingRepository.save(booking);
				});
				log.info("Cancelled booking {} and released room {}", id, booking.getRoomId());
			} catch (Exception e) {
				log.error("Failed to release lock for booking {}: {}. Booking remains in CANCELLING state.", id,
						e.getMessage());
			}
		} else {
			transactionTemplate.execute(status -> {
				booking.finalizeCancellation();
				return bookingRepository.save(booking);
			});
			log.info("Cancelled booking {} (no lock to release)", id);
		}
	}

	@Scheduled(fixedDelay = 60000)
	public void reconcileStuckBookings() {
		LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
		List<Booking> stuckPending = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, threshold);
		for (Booking booking : stuckPending) {
			log.warn("Reconciling stuck PENDING booking {}", booking.getId());
			transactionTemplate.execute(status -> {
				booking.cancel();
				return bookingRepository.save(booking);
			});
			try {
				hotelServiceClient.releaseLock(booking.getRoomId(), booking.getRequestId(),
						serviceTokenProvider.getServiceToken());
			} catch (Exception e) {
				log.error("Failed to release lock during reconciliation for booking {}", booking.getId(), e);
			}
		}

		List<Booking> cancelling = bookingRepository.findByStatus(BookingStatus.CANCELLING);
		for (Booking booking : cancelling) {
			try {
				hotelServiceClient.releaseLock(booking.getRoomId(), booking.getRequestId(),
						serviceTokenProvider.getServiceToken());
				transactionTemplate.execute(status -> {
					booking.finalizeCancellation();
					return bookingRepository.save(booking);
				});
				log.info("Successfully retried cancellation for booking {}", booking.getId());
			} catch (Exception e) {
				log.error("Retry failed for booking {}: {}", booking.getId(), e.getMessage());
			}
		}
	}

	private Booking findBookingOrThrow(Long id) {
		return bookingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Booking", id));
	}
}
