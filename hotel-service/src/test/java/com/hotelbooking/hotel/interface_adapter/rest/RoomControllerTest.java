package com.hotelbooking.hotel.interface_adapter.rest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hotelbooking.hotel.application.dto.request.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.application.dto.request.CreateRoomRequest;
import com.hotelbooking.hotel.application.dto.response.RoomResponse;
import com.hotelbooking.hotel.application.service.RoomService;
import com.hotelbooking.hotel.domain.exception.RoomAlreadyLockedException;
import com.hotelbooking.hotel.infrastructure.config.SecurityConfig;
import com.hotelbooking.hotel.interface_adapter.rest.error.GlobalExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("RoomController")
class RoomControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RoomService roomService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	@DisplayName("should return 401 when not authenticated")
	void getFreeRooms_whenNotAuthenticated_shouldReturn401() throws Exception {
		mockMvc.perform(get("/api/rooms").param("startDate", LocalDate.now().plusDays(1).toString()).param("endDate",
				LocalDate.now().plusDays(3).toString())).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "USER")
	@DisplayName("should return free rooms when authenticated")
	void getFreeRooms_whenAuthenticated_shouldReturnRooms() throws Exception {
		RoomResponse room = new RoomResponse(1L, 1L, "101", true, 0L);
		Page<RoomResponse> page = new PageImpl<>(List.of(room));
		when(roomService.findFreeRooms(any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
				.thenReturn(page);

		mockMvc.perform(get("/api/rooms").param("startDate", LocalDate.now().plusDays(1).toString()).param("endDate",
				LocalDate.now().plusDays(3).toString())).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].number").value("101"));
	}

	@Test
	@WithMockUser(roles = "USER")
	@DisplayName("should return recommended rooms sorted by timesBooked")
	void getRecommendedRooms_shouldReturnSortedRooms() throws Exception {
		RoomResponse room1 = new RoomResponse(1L, 1L, "101", true, 0L);
		RoomResponse room2 = new RoomResponse(2L, 1L, "102", true, 5L);
		Page<RoomResponse> page = new PageImpl<>(List.of(room1, room2));
		when(roomService.findRecommendedRooms(any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
				.thenReturn(page);

		mockMvc.perform(get("/api/rooms/recommend").param("startDate", LocalDate.now().plusDays(1).toString())
				.param("endDate", LocalDate.now().plusDays(3).toString())).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].timesBooked").value(0))
				.andExpect(jsonPath("$.content[1].timesBooked").value(5));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DisplayName("should create room when admin")
	void createRoom_whenAdmin_shouldCreateRoom() throws Exception {
		CreateRoomRequest request = new CreateRoomRequest(1L, "101");
		RoomResponse response = new RoomResponse(1L, 1L, "101", true, 0L);
		when(roomService.create(any(CreateRoomRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/rooms").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.number").value("101"));
	}

	@Test
	@WithMockUser(roles = "USER")
	@DisplayName("should return 403 when user tries to create room")
	void createRoom_whenUser_shouldReturn403() throws Exception {
		CreateRoomRequest request = new CreateRoomRequest(1L, "101");

		mockMvc.perform(post("/api/rooms").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "SERVICE")
	@DisplayName("should return 409 when room already locked")
	void confirmAvailability_whenRoomAlreadyLocked_shouldReturn409() throws Exception {
		Long roomId = 1L;
		LocalDate startDate = LocalDate.now().plusDays(1);
		LocalDate endDate = LocalDate.now().plusDays(3);
		ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest(startDate, endDate, "request-id");

		doThrow(new RoomAlreadyLockedException(roomId)).when(roomService).confirmAvailability(eq(roomId),
				any(ConfirmAvailabilityRequest.class));

		mockMvc.perform(post("/api/rooms/{id}/confirm-availability", roomId).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
	}
}
