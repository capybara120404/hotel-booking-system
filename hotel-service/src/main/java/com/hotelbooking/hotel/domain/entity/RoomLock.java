package com.hotelbooking.hotel.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_locks", uniqueConstraints = @UniqueConstraint(name = "uk_room_locks_request", columnNames = "request_id"), indexes = @Index(name = "idx_room_locks_room_dates", columnList = "room_id,start_date,end_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomLock {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "request_id", nullable = false)
	private String requestId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private LockStatus status;

	public RoomLock(Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {
		if (roomId == null) {
			throw new IllegalArgumentException("Room id cannot be null");
		}
		if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
			throw new IllegalArgumentException("Invalid booking date range");
		}
		if (requestId == null || requestId.isBlank()) {
			throw new IllegalArgumentException("Request id cannot be blank");
		}
		this.roomId = roomId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.requestId = requestId;
		this.createdAt = LocalDateTime.now();
		this.expiresAt = this.createdAt.plusMinutes(5);
		this.status = LockStatus.HELD;
	}

	public void confirm() {
		this.status = LockStatus.CONFIRMED;
		this.expiresAt = null;
	}

	public enum LockStatus {
		HELD, CONFIRMED
	}
}
