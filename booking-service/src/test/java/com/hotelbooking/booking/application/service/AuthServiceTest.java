package com.hotelbooking.booking.application.service;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hotelbooking.booking.application.dto.request.AuthRequest;
import com.hotelbooking.booking.application.dto.request.RegisterRequest;
import com.hotelbooking.booking.application.dto.response.AuthResponse;
import com.hotelbooking.booking.domain.entity.Role;
import com.hotelbooking.booking.domain.entity.User;
import com.hotelbooking.booking.infrastructure.persistence.UserRepository;
import com.hotelbooking.booking.infrastructure.security.AuthenticatedUser;
import com.hotelbooking.booking.infrastructure.security.JwtTokenProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserDetailsService userDetailsService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, authenticationManager,
				userDetailsService);
	}

	@Test
	@DisplayName("should register user and return token")
	void register_shouldCreateUserAndReturnToken() {
		RegisterRequest request = new RegisterRequest("testuser", "password123");
		User savedUser = new User("testuser", "encodedPassword", Role.USER);
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "testuser", "encodedPassword",
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		when(userRepository.existsByUsername("testuser")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenReturn(savedUser);
		when(userDetailsService.loadUserByUsername("testuser")).thenReturn(authenticatedUser);
		when(jwtTokenProvider.generateToken(authenticatedUser)).thenReturn("jwt-token");

		AuthResponse response = authService.register(request);

		assertThat(response.token()).isEqualTo("jwt-token");
		verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("should throw exception when username already exists")
	void register_whenUsernameExists_shouldThrowException() {
		RegisterRequest request = new RegisterRequest("existinguser", "password123");
		when(userRepository.existsByUsername("existinguser")).thenReturn(true);
		assertThatThrownBy(() -> authService.register(request)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Username already exists");
	}

	@Test
	@DisplayName("should authenticate user and return token")
	void authenticate_shouldReturnToken() {
		AuthRequest request = new AuthRequest("testuser", "password123");
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(1L, "testuser", "encodedPassword",
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		when(userDetailsService.loadUserByUsername("testuser")).thenReturn(authenticatedUser);
		when(jwtTokenProvider.generateToken(authenticatedUser)).thenReturn("jwt-token");

		AuthResponse response = authService.authenticate(request);

		assertThat(response.token()).isEqualTo("jwt-token");
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
	}
}
