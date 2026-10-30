package com.hotelbooking.booking.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookings", uniqueConstraints = {
		@UniqueConstraint(name = "uk_bookings_request_id", columnNames = "request_id"),
		@UniqueConstraint(name = "uk_bookings_user_idempotency", columnNames = {"user_id", "idempotency_key"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status;

	@Column(name = "request_id")
	private String requestId;

	@Column(name = "idempotency_key")
	private String idempotencyKey;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public Booking(Long userId, Long roomId, LocalDate startDate, LocalDate endDate, String requestId,
			String idempotencyKey) {
		if (userId == null) {
			throw new IllegalArgumentException("UserId cannot be null");
		}
		if (roomId == null) {
			throw new IllegalArgumentException("RoomId cannot be null");
		}
		if (startDate == null || endDate == null) {
			throw new IllegalArgumentException("Dates cannot be null");
		}
		if (!startDate.isBefore(endDate)) {
			throw new IllegalArgumentException("Start date must be before end date");
		}
		if (requestId == null || requestId.isBlank()) {
			throw new IllegalArgumentException("RequestId cannot be blank");
		}
		this.userId = userId;
		this.roomId = roomId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.status = BookingStatus.PENDING;
		this.requestId = requestId;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = LocalDateTime.now();
	}

	public void updateForRetry(Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {
		if (this.status != BookingStatus.CANCELLED) {
			throw new IllegalStateException("Only CANCELLED bookings can be updated for retry");
		}
		this.roomId = roomId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.requestId = requestId;
		this.status = BookingStatus.PENDING;
		this.createdAt = LocalDateTime.now();
	}

	public void confirm() {
		if (this.status != BookingStatus.PENDING) {
			throw new IllegalStateException("Only PENDING bookings can be confirmed");
		}
		this.status = BookingStatus.CONFIRMED;
	}

	public void markCancelling() {
		if (this.status == BookingStatus.CANCELLED || this.status == BookingStatus.CANCELLING) {
			return;
		}
		if (this.status != BookingStatus.CONFIRMED && this.status != BookingStatus.PENDING) {
			throw new IllegalStateException("Only CONFIRMED or PENDING bookings can be cancelled");
		}
		this.status = BookingStatus.CANCELLING;
	}

	public void finalizeCancellation() {
		if (this.status != BookingStatus.CANCELLING) {
			throw new IllegalStateException("Only CANCELLING bookings can be finalized");
		}
		this.status = BookingStatus.CANCELLED;
	}

	public void cancel() {
		if (this.status == BookingStatus.CANCELLED) {
			return;
		}
		this.status = BookingStatus.CANCELLED;
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}
}
