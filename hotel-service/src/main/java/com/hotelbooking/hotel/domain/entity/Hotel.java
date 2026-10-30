package com.hotelbooking.hotel.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hotels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hotel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String address;

	public Hotel(String name, String address) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Hotel name cannot be blank");
		}
		if (address == null || address.isBlank()) {
			throw new IllegalArgumentException("Hotel address cannot be blank");
		}
		this.name = name;
		this.address = address;
	}

	public void update(String name, String address) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Hotel name cannot be blank");
		}
		if (address == null || address.isBlank()) {
			throw new IllegalArgumentException("Hotel address cannot be blank");
		}
		this.name = name;
		this.address = address;
	}
}
