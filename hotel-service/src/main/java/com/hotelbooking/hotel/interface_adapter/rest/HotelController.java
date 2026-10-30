package com.hotelbooking.hotel.interface_adapter.rest;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.hotelbooking.hotel.application.dto.request.CreateHotelRequest;
import com.hotelbooking.hotel.application.dto.request.UpdateHotelRequest;
import com.hotelbooking.hotel.application.dto.response.HotelResponse;
import com.hotelbooking.hotel.application.dto.response.OccupancyStatsDto;
import com.hotelbooking.hotel.application.service.HotelService;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

	private final HotelService hotelService;

	public HotelController(HotelService hotelService) {
		this.hotelService = hotelService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<HotelResponse> create(@Valid @RequestBody CreateHotelRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.create(request));
	}

	@GetMapping
	public ResponseEntity<Page<HotelResponse>> findAll(@RequestParam(required = false) String name, Pageable pageable) {
		return ResponseEntity.ok(hotelService.findAll(name, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<HotelResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(hotelService.findById(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<HotelResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateHotelRequest request) {
		return ResponseEntity.ok(hotelService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		hotelService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/occupancy-stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<OccupancyStatsDto> getOccupancyStats() {
		return ResponseEntity.ok(hotelService.getOccupancyStats());
	}
}
