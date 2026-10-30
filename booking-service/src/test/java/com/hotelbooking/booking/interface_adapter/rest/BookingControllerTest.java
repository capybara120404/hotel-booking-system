package com.hotelbooking.booking.interface_adapter.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hotelbooking.booking.application.dto.request.CreateBookingRequest;
import com.hotelbooking.booking.application.dto.response.BookingResponse;
import com.hotelbooking.booking.application.service.BookingOrchestrator;
import com.hotelbooking.booking.application.service.BookingService;
import com.hotelbooking.booking.domain.entity.BookingStatus;
import com.hotelbooking.booking.infrastructure.config.SecurityConfig;
import com.hotelbooking.booking.infrastructure.security.AuthenticatedUser;
import com.hotelbooking.booking.infrastructure.security.JwtTokenProvider;
import com.hotelbooking.booking.interface_adapter.rest.error.GlobalExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("BookingController")
class BookingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private BookingOrchestrator bookingOrchestrator;

	@MockitoBean
	private BookingService bookingService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private UserDetailsService userDetailsService;

	private void setupMockAuth(String token, String username, String role, Long id) {
		when(jwtTokenProvider.extractUsername(token)).thenReturn(username);
		AuthenticatedUser user = new AuthenticatedUser(id, username, "password",
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
		when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
		when(jwtTokenProvider.validateToken(token, user)).thenReturn(true);
	}

	@Test
	@DisplayName("should return 401 when not authenticated")
	void createBooking_whenNotAuthenticated_shouldReturn401() throws Exception {
		CreateBookingRequest request = new CreateBookingRequest(1L, LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(3), false);
		when(jwtTokenProvider.extractUsername("invalid-token")).thenThrow(new JwtException("Invalid"));
		mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer invalid-token")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("should create booking when authenticated")
	void createBooking_whenAuthenticated_shouldCreateBooking() throws Exception {
		String token = "valid-token";
		setupMockAuth(token, "user1", "USER", 1L);
		CreateBookingRequest request = new CreateBookingRequest(1L, LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(3), false);
		BookingResponse response = new BookingResponse(1L, 1L, 1L, request.startDate(), request.endDate(),
				BookingStatus.CONFIRMED, LocalDateTime.now());
		when(bookingOrchestrator.createBooking(eq(1L), any(CreateBookingRequest.class), any(), eq(token)))
				.thenReturn(response);
		mockMvc.perform(post("/api/bookings").with(csrf()).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	@DisplayName("should return user's bookings")
	void getUserBookings_shouldReturnOnlyUserBookings() throws Exception {
		String token = "valid-token";
		setupMockAuth(token, "user1", "USER", 1L);
		BookingResponse booking = new BookingResponse(1L, 1L, 1L, LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(3), BookingStatus.CONFIRMED, LocalDateTime.now());
		Page<BookingResponse> page = new PageImpl<>(List.of(booking));
		when(bookingService.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
		mockMvc.perform(get("/api/bookings").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("should cancel own booking")
	void cancelBooking_whenOwnBooking_shouldCancel() throws Exception {
		String token = "valid-token";
		setupMockAuth(token, "user1", "USER", 1L);
		doNothing().when(bookingService).cancelBooking(eq(1L), eq(1L));
		mockMvc.perform(delete("/api/bookings/1").with(csrf()).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("should return 403 when trying to cancel another user's booking")
	void cancelBooking_whenNotOwnBooking_shouldReturn403() throws Exception {
		String token = "valid-token";
		setupMockAuth(token, "user2", "USER", 2L);
		doThrow(new org.springframework.security.access.AccessDeniedException("Access denied")).when(bookingService)
				.cancelBooking(eq(1L), eq(2L));
		mockMvc.perform(delete("/api/bookings/1").with(csrf()).header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("should return 403 when admin tries to access user endpoints")
	void createBooking_whenAdmin_shouldReturn403() throws Exception {
		String token = "admin-token";
		setupMockAuth(token, "admin", "ADMIN", 3L);
		CreateBookingRequest request = new CreateBookingRequest(1L, LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(3), false);
		mockMvc.perform(post("/api/bookings").with(csrf()).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
	}
}
