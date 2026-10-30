package com.hotelbooking.booking.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.hotelbooking.booking.application.dto.request.CreateBookingRequest;
import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.application.dto.response.RoomResponse;
import com.hotelbooking.booking.application.mapper.BookingMapper;
import com.hotelbooking.booking.application.port.HotelServiceClient;
import com.hotelbooking.booking.domain.entity.Booking;
import com.hotelbooking.booking.domain.entity.BookingStatus;
import com.hotelbooking.booking.domain.exception.HotelServiceUnavailableException;
import com.hotelbooking.booking.domain.exception.RoomNotAvailableException;
import com.hotelbooking.booking.infrastructure.persistence.BookingRepository;
import com.hotelbooking.booking.infrastructure.security.ServiceTokenProvider;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Service
public class BookingOrchestrator {
	private static final Logger log = LoggerFactory.getLogger(BookingOrchestrator.class);
	private static final String BOOKING_ID_MDC_KEY = "bookingId";

	private final BookingRepository bookingRepository;
	private final HotelServiceClient hotelServiceClient;
	private final BookingMapper bookingMapper;
	private final TransactionTemplate transactionTemplate;
	private final ServiceTokenProvider serviceTokenProvider;

	public BookingOrchestrator(BookingRepository bookingRepository, HotelServiceClient hotelServiceClient,
			BookingMapper bookingMapper, TransactionTemplate transactionTemplate,
			ServiceTokenProvider serviceTokenProvider) {
		this.bookingRepository = bookingRepository;
		this.hotelServiceClient = hotelServiceClient;
		this.bookingMapper = bookingMapper;
		this.transactionTemplate = transactionTemplate;
		this.serviceTokenProvider = serviceTokenProvider;
	}

	public BookingResponse createBooking(Long userId, CreateBookingRequest request, String idempotencyKey,
			String token) {
		List<Long> candidateRooms = resolveCandidateRooms(request, token);
		String serviceToken = serviceTokenProvider.getServiceToken();
		Exception lastFailure = null;
		Booking currentBooking = null;

		for (Long roomId : candidateRooms) {
			String requestId = UUID.randomUUID().toString();
			try {
				currentBooking = transactionTemplate.execute(status -> {
					if (idempotencyKey != null && !idempotencyKey.isBlank()) {
						Optional<Booking> opt = bookingRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId);
						if (opt.isPresent()) {
							Booking b = opt.get();
							if (b.getStatus() == BookingStatus.CONFIRMED) {
								return b;
							}
							b.updateForRetry(roomId, request.startDate(), request.endDate(), requestId);
							return bookingRepository.save(b);
						}
					}
					return bookingRepository.save(new Booking(userId, roomId, request.startDate(), request.endDate(),
							requestId, idempotencyKey));
				});

				if (currentBooking.getStatus() == BookingStatus.CONFIRMED) {
					return bookingMapper.toResponse(currentBooking);
				}

				MDC.put(BOOKING_ID_MDC_KEY, String.valueOf(currentBooking.getId()));
				log.info("Created/Updated booking {} in PENDING status for room {} with requestId {}",
						currentBooking.getId(), roomId, requestId);

				Booking confirmedBooking = confirmAndCommit(currentBooking, roomId, request, requestId, serviceToken);
				return bookingMapper.toResponse(confirmedBooking);

			} catch (CallNotPermittedException e) {
				log.error("Circuit breaker is open for hotel service. Aborting candidate search for booking {}.",
						currentBooking != null ? currentBooking.getId() : "unknown");
				if (currentBooking != null) {
					compensate(currentBooking, roomId, requestId, serviceToken);
				}
				throw new HotelServiceUnavailableException(
						"Hotel service is temporarily unavailable (Circuit Breaker open)", e);
			} catch (HotelServiceUnavailableException e) {
				log.error("Hotel service unavailable for booking {}. Aborting candidate search.",
						currentBooking != null ? currentBooking.getId() : "unknown");
				if (currentBooking != null) {
					compensate(currentBooking, roomId, requestId, serviceToken);
				}
				throw e;
			} catch (Exception e) {
				log.error("Failed to confirm booking {} for room {}: {}. Initiating compensation.",
						currentBooking != null ? currentBooking.getId() : "unknown", roomId, e.getMessage());
				if (currentBooking != null) {
					compensate(currentBooking, roomId, requestId, serviceToken);
				}
				lastFailure = e;
			} finally {
				MDC.remove(BOOKING_ID_MDC_KEY);
			}
			log.warn("Room {} not available, trying next candidate.", roomId);
		}

		if (lastFailure instanceof HotelServiceUnavailableException) {
			throw (HotelServiceUnavailableException) lastFailure;
		}
		throw new RoomNotAvailableException(null);
	}

	private Booking confirmAndCommit(Booking booking, Long roomId, CreateBookingRequest request, String requestId,
			String serviceToken) {
		hotelServiceClient.confirmAvailability(roomId, request.startDate(), request.endDate(), requestId, serviceToken);

		Booking confirmed = transactionTemplate.execute(status -> {
			Booking b = bookingRepository.findById(booking.getId())
					.orElseThrow(() -> new IllegalStateException("Booking disappeared: " + booking.getId()));
			b.confirm();
			return bookingRepository.save(b);
		});

		try {
			hotelServiceClient.commitLock(roomId, requestId, serviceToken);
		} catch (Exception e) {
			log.error("Failed to commit lock for booking {}. Initiating compensation.", booking.getId());
			compensate(booking, roomId, requestId, serviceToken);
			throw new HotelServiceUnavailableException("Failed to commit lock", e);
		}
		return confirmed;
	}

	private void compensate(Booking booking, Long roomId, String requestId, String serviceToken) {
		transactionTemplate.execute(status -> {
			Booking b = bookingRepository.findById(booking.getId())
					.orElseThrow(() -> new IllegalStateException("Booking disappeared during cancel"));
			b.markCancelling();
			return bookingRepository.save(b);
		});
		try {
			hotelServiceClient.releaseLock(roomId, requestId, serviceToken);
			transactionTemplate.execute(status -> {
				Booking b = bookingRepository.findById(booking.getId())
						.orElseThrow(() -> new IllegalStateException("Booking disappeared during finalize"));
				b.finalizeCancellation();
				return bookingRepository.save(b);
			});
			log.info("Compensation completed for booking {}", booking.getId());
		} catch (Exception compensationException) {
			log.error(
					"Compensation failed for booking {} and room {}: {}. Booking remains in CANCELLING state for reconciliation.",
					booking.getId(), roomId, compensationException.getMessage(), compensationException);
		}
	}

	private List<Long> resolveCandidateRooms(CreateBookingRequest request, String token) {
		if (request.autoSelect()) {
			List<RoomResponse> recommended = hotelServiceClient.getRecommendedRooms(request.startDate(),
					request.endDate(), token);
			if (recommended.isEmpty()) {
				throw new RoomNotAvailableException(null);
			}
			return recommended.stream().map(RoomResponse::id).toList();
		}
		if (request.roomId() == null) {
			throw new IllegalArgumentException("Room ID is required when autoSelect is false");
		}
		return List.of(request.roomId());
	}
}
