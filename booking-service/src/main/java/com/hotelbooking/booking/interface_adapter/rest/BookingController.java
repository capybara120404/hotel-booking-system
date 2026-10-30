package com.hotelbooking.booking.interface_adapter.rest;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.booking.application.dto.request.CreateBookingRequest;
import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.application.service.BookingOrchestrator;
import com.hotelbooking.booking.application.service.BookingService;
import com.hotelbooking.booking.infrastructure.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
	private final BookingOrchestrator bookingOrchestrator;
	private final BookingService bookingService;

	public BookingController(BookingOrchestrator bookingOrchestrator, BookingService bookingService) {
		this.bookingOrchestrator = bookingOrchestrator;
		this.bookingService = bookingService;
	}

	@PostMapping
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody CreateBookingRequest request, @RequestHeader("Authorization") String authHeader,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
		String token = authHeader.substring(7);
		BookingResponse response = bookingOrchestrator.createBooking(user.getId(), request, idempotencyKey, token);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<Page<BookingResponse>> findAll(@AuthenticationPrincipal AuthenticatedUser user,
			Pageable pageable) {
		return ResponseEntity.ok(bookingService.findByUserId(user.getId(), pageable));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<BookingResponse> findById(@PathVariable Long id,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ResponseEntity.ok(bookingService.findById(id, user.getId()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
		bookingService.cancelBooking(id, user.getId());
		return ResponseEntity.noContent().build();
	}
}
