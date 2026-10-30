package com.hotelbooking.booking.interface_adapter.rest.error;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.hotelbooking.booking.domain.exception.EntityNotFoundException;
import com.hotelbooking.booking.domain.exception.HotelServiceUnavailableException;
import com.hotelbooking.booking.domain.exception.RoomNotAvailableException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ErrorDto> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorDto> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Resource not found",
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED",
				"Method " + ex.getMethod() + " not supported", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
	}

	@ExceptionHandler(RoomNotAvailableException.class)
	public ResponseEntity<ErrorDto> handleUnavailable(RoomNotAvailableException ex, HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.CONFLICT.value(), "ROOM_UNAVAILABLE", ex.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(HotelServiceUnavailableException.class)
	public ResponseEntity<ErrorDto> handleHotelUnavailable(HotelServiceUnavailableException ex,
			HttpServletRequest request) {
		log.warn("Hotel service unavailable on {}: {}", request.getRequestURI(), ex.getMessage());
		ErrorDto error = ErrorDto.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "HOTEL_SERVICE_UNAVAILABLE",
				"Hotel service is temporarily unavailable", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
	}

	@ExceptionHandler(CallNotPermittedException.class)
	public ResponseEntity<ErrorDto> handleCircuitBreakerOpen(CallNotPermittedException ex, HttpServletRequest request) {
		log.warn("Circuit breaker open for hotel service: {}", ex.getMessage());
		ErrorDto error = ErrorDto.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "HOTEL_SERVICE_UNAVAILABLE",
				"Hotel service is temporarily unavailable", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorDto> handleDataIntegrityViolation(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		String message = ex.getMessage();
		if (message != null && message.toLowerCase().contains("idempotency_key")) {
			ErrorDto error = ErrorDto.of(HttpStatus.CONFLICT.value(), "CONFLICT",
					"Booking with this idempotency key already exists", request.getRequestURI());
			return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
		}
		ErrorDto error = ErrorDto.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", "Data integrity violation",
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ErrorDto.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ErrorDto.FieldError(fe.getField(), fe.getDefaultMessage())).toList();
		ErrorDto error = ErrorDto.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Request validation failed",
				request.getRequestURI(), fieldErrors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorDto> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Access denied",
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorDto> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", "Authentication required",
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorDto> handleMissingParam(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorDto> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", "Invalid request body",
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		ErrorDto error = ErrorDto.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST",
				"Invalid parameter type: " + ex.getName(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorDto> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
		ErrorDto error = ErrorDto.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
				"An unexpected error occurred", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
