package com.hotelbooking.hotel.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_rooms_hotel_number", columnNames = {"hotel_id",
		"number"}), indexes = @Index(name = "idx_rooms_hotel", columnList = "hotel_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "hotel_id", nullable = false)
	private Long hotelId;

	@Column(nullable = false)
	private String number;

	@Column(nullable = false)
	private boolean available;

	@Column(name = "times_booked", nullable = false)
	private long timesBooked;

	@Version
	private Long version;

	public Room(Long hotelId, String number) {
		if (hotelId == null) {
			throw new IllegalArgumentException("Hotel id cannot be null");
		}
		if (number == null || number.isBlank()) {
			throw new IllegalArgumentException("Room number cannot be blank");
		}
		this.hotelId = hotelId;
		this.number = number;
		this.available = true;
		this.timesBooked = 0L;
	}

	public void update(String number, boolean available) {
		if (number == null || number.isBlank()) {
			throw new IllegalArgumentException("Room number cannot be blank");
		}
		this.number = number;
		this.available = available;
	}

	public void incrementBookings() {
		this.timesBooked = Math.addExact(this.timesBooked, 1L);
	}
}
