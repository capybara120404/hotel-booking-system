package com.hotelbooking.booking.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.booking.application.dto.request.AuthRequest;
import com.hotelbooking.booking.application.dto.request.RegisterRequest;
import com.hotelbooking.booking.application.dto.response.AuthResponse;
import com.hotelbooking.booking.domain.entity.Role;
import com.hotelbooking.booking.domain.entity.User;
import com.hotelbooking.booking.infrastructure.persistence.UserRepository;
import com.hotelbooking.booking.infrastructure.security.AuthenticatedUser;
import com.hotelbooking.booking.infrastructure.security.JwtTokenProvider;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager,
			UserDetailsService userDetailsService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new IllegalArgumentException("Username already exists");
		}
		User user = new User(request.username(), passwordEncoder.encode(request.password()), Role.USER);
		userRepository.save(user);
		UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
		if (!(userDetails instanceof AuthenticatedUser authenticatedUser)) {
			throw new IllegalStateException("Expected AuthenticatedUser");
		}
		String token = jwtTokenProvider.generateToken(authenticatedUser);
		return new AuthResponse(token);
	}

	public AuthResponse authenticate(AuthRequest request) {
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
		if (!(userDetails instanceof AuthenticatedUser authenticatedUser)) {
			throw new IllegalStateException("Expected AuthenticatedUser");
		}
		String token = jwtTokenProvider.generateToken(authenticatedUser);
		return new AuthResponse(token);
	}
}
