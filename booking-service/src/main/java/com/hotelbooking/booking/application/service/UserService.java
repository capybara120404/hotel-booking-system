package com.hotelbooking.booking.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.booking.application.dto.request.CreateUserRequest;
import com.hotelbooking.booking.application.dto.request.UpdateUserRequest;
import com.hotelbooking.booking.application.dto.response.UserResponse;
import com.hotelbooking.booking.application.mapper.UserMapper;
import com.hotelbooking.booking.domain.entity.Role;
import com.hotelbooking.booking.domain.entity.User;
import com.hotelbooking.booking.domain.exception.EntityNotFoundException;
import com.hotelbooking.booking.infrastructure.persistence.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new IllegalArgumentException("Username already exists");
		}
		User user = new User(request.username(), passwordEncoder.encode(request.password()),
				Role.valueOf(request.role()));
		return userMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public UserResponse update(Long id, UpdateUserRequest request) {
		User user = findUserOrThrow(id);
		user.update(passwordEncoder.encode(request.password()), Role.valueOf(request.role()));
		return userMapper.toResponse(user);
	}

	@Transactional
	public void delete(Long id) {
		User user = findUserOrThrow(id);
		userRepository.delete(user);
	}

	private User findUserOrThrow(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User", id));
	}
}
