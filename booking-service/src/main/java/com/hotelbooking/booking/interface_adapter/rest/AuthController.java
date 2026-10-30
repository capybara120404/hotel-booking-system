package com.hotelbooking.booking.interface_adapter.rest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.booking.application.dto.request.AuthRequest;
import com.hotelbooking.booking.application.dto.request.RegisterRequest;
import com.hotelbooking.booking.application.dto.response.AuthResponse;
import com.hotelbooking.booking.application.service.AuthService;

@RestController
@RequestMapping("/user")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = authService.register(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/auth")
	public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
		AuthResponse response = authService.authenticate(request);
		return ResponseEntity.ok(response);
	}
}
