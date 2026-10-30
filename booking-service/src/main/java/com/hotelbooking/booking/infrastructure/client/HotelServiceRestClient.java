package com.hotelbooking.booking.infrastructure.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.hotelbooking.booking.application.dto.response.RoomResponse;
import com.hotelbooking.booking.application.port.HotelServiceClient;
import com.hotelbooking.booking.domain.exception.HotelServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class HotelServiceRestClient implements HotelServiceClient {
	private static final Logger log = LoggerFactory.getLogger(HotelServiceRestClient.class);
	private static final String HOTEL_SERVICE_URL = "http://hotel-service";
	private final RestClient restClient;

	public HotelServiceRestClient(RestClient.Builder restClientBuilder,
			@Value("${http.client.connect-timeout:2000}") int connectTimeout,
			@Value("${http.client.read-timeout:5000}") int readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		this.restClient = restClientBuilder.baseUrl(HOTEL_SERVICE_URL).requestFactory(factory).build();
	}

	@Override
	@Retry(name = "hotelAvailability")
	@CircuitBreaker(name = "hotelAvailability")
	public List<RoomResponse> getRecommendedRooms(LocalDate startDate, LocalDate endDate, String token) {
		log.info("Fetching recommended rooms from {} to {}", startDate, endDate);
		try {
			RoomPageResponse response = restClient.get()
					.uri("/api/rooms/recommend?startDate={startDate}&endDate={endDate}", startDate, endDate)
					.headers(h -> addHeaders(h, token)).retrieve()
					.onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
						throw new HotelServiceUnavailableException("Hotel service returned " + res.getStatusCode());
					}).body(new ParameterizedTypeReference<RoomPageResponse>() {
					});
			return response == null || response.content() == null ? List.of() : response.content();
		} catch (ResourceAccessException e) {
			throw new HotelServiceUnavailableException("Hotel service unreachable: " + e.getMessage(), e);
		}
	}

	@Override
	@Retry(name = "hotelAvailability")
	@CircuitBreaker(name = "hotelAvailability")
	public void confirmAvailability(Long roomId, LocalDate startDate, LocalDate endDate, String requestId,
			String token) {
		log.info("Confirming availability for room {} from {} to {} with requestId {}", roomId, startDate, endDate,
				requestId);
		Map<String, String> request = Map.of("startDate", startDate.toString(), "endDate", endDate.toString(),
				"requestId", requestId);
		try {
			restClient.post().uri("/api/rooms/{roomId}/confirm-availability", roomId).headers(h -> addHeaders(h, token))
					.body(request).retrieve().onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
						throw new HotelServiceUnavailableException("Hotel service returned " + res.getStatusCode());
					}).toBodilessEntity();
		} catch (ResourceAccessException e) {
			throw new HotelServiceUnavailableException("Hotel service unreachable: " + e.getMessage(), e);
		}
	}

	@Override
	@Retry(name = "hotelAvailability")
	@CircuitBreaker(name = "hotelAvailability")
	public void commitLock(Long roomId, String requestId, String token) {
		log.info("Committing lock for room {} with requestId {}", roomId, requestId);
		try {
			restClient.post().uri("/api/rooms/{roomId}/commit?requestId={requestId}", roomId, requestId)
					.headers(h -> addHeaders(h, token)).retrieve().toBodilessEntity();
		} catch (ResourceAccessException e) {
			throw new HotelServiceUnavailableException("Hotel service unreachable during commit: " + e.getMessage(), e);
		}
	}

	@Override
	public void releaseLock(Long roomId, String requestId, String token) {
		log.info("Releasing lock for room {} with requestId {}", roomId, requestId);
		try {
			restClient.post().uri("/api/rooms/{roomId}/release?requestId={requestId}", roomId, requestId)
					.headers(h -> addHeaders(h, token)).retrieve().toBodilessEntity();
		} catch (Exception e) {
			log.error("Failed to release lock for room {} with requestId {}: {}", roomId, requestId, e.getMessage());
			throw new HotelServiceUnavailableException("Failed to release lock: " + e.getMessage(), e);
		}
	}

	private void addHeaders(org.springframework.http.HttpHeaders h, String token) {
		h.setBearerAuth(token);
		String traceId = MDC.get("traceId");
		if (traceId != null) {
			h.set("X-Request-Id", traceId);
		}
	}

	private record RoomPageResponse(List<RoomResponse> content) {
	}
}
