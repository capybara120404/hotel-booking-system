package com.hotelbooking.booking.interface_adapter.rest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.booking.application.dto.request.CreateUserRequest;
import com.hotelbooking.booking.application.dto.request.UpdateUserRequest;
import com.hotelbooking.booking.application.dto.response.UserResponse;
import com.hotelbooking.booking.application.service.UserService;

@RestController
@RequestMapping("/user")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(userService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
