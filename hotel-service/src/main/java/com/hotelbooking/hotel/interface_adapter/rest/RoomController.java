package com.hotelbooking.hotel.interface_adapter.rest;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.hotel.application.dto.request.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.application.dto.request.CreateRoomRequest;
import com.hotelbooking.hotel.application.dto.request.UpdateRoomRequest;
import com.hotelbooking.hotel.application.dto.response.RoomResponse;
import com.hotelbooking.hotel.application.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
	}

	@GetMapping
	public ResponseEntity<Page<RoomResponse>> findFreeRooms(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, Pageable pageable) {
		return ResponseEntity.ok(roomService.findFreeRooms(startDate, endDate, pageable));
	}

	@GetMapping("/recommend")
	public ResponseEntity<Page<RoomResponse>> findRecommendedRooms(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, Pageable pageable) {
		return ResponseEntity.ok(roomService.findRecommendedRooms(startDate, endDate, pageable));
	}

	@GetMapping("/hotel/{hotelId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Page<RoomResponse>> findByHotel(@PathVariable Long hotelId, Pageable pageable) {
		return ResponseEntity.ok(roomService.findByHotel(hotelId, pageable));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RoomResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateRoomRequest request) {
		return ResponseEntity.ok(roomService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		roomService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/confirm-availability")
	@PreAuthorize("hasRole('SERVICE')")
	public ResponseEntity<Void> confirmAvailability(@PathVariable Long id,
			@Valid @RequestBody ConfirmAvailabilityRequest request) {
		roomService.confirmAvailability(id, request);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/commit")
	@PreAuthorize("hasRole('SERVICE')")
	public ResponseEntity<Void> commit(@PathVariable Long id, @RequestParam String requestId) {
		roomService.commitLock(id, requestId);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/release")
	@PreAuthorize("hasRole('SERVICE')")
	public ResponseEntity<Void> release(@PathVariable Long id, @RequestParam String requestId) {
		roomService.releaseLock(id, requestId);
		return ResponseEntity.ok().build();
	}
}
