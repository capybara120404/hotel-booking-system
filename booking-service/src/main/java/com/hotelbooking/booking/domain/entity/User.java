package com.hotelbooking.booking.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	public User(String username, String encodedPassword, Role role) {
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("Username cannot be blank");
		}
		if (encodedPassword == null || encodedPassword.isBlank()) {
			throw new IllegalArgumentException("Password cannot be blank");
		}
		if (role == null) {
			throw new IllegalArgumentException("Role cannot be null");
		}

		this.username = username;
		this.password = encodedPassword;
		this.role = role;
	}

	public void update(String encodedPassword, Role role) {
		if (encodedPassword == null || encodedPassword.isBlank()) {
			throw new IllegalArgumentException("Password cannot be blank");
		}
		if (role == null) {
			throw new IllegalArgumentException("Role cannot be null");
		}

		this.password = encodedPassword;
		this.role = role;
	}
}
