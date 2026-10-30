package com.hotelbooking.booking.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelbooking.booking.domain.entity.Booking;
import com.hotelbooking.booking.domain.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {
	Page<Booking> findByUserId(Long userId, Pageable pageable);
	Optional<Booking> findByIdempotencyKeyAndUserId(String idempotencyKey, Long userId);
	List<Booking> findByStatus(BookingStatus status);
	List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime createdAt);
}
