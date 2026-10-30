package com.hotelbooking.booking.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hotelbooking.booking.interface_adapter.rest.error.ErrorDto;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtTokenProvider jwtTokenProvider;
	private final UserDetailsService userDetailsService;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService,
			ObjectMapper objectMapper) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userDetailsService = userDetailsService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		final String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		final String jwt = authHeader.substring(7);
		try {
			final String username = jwtTokenProvider.extractUsername(jwt);
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
				if (userDetails instanceof AuthenticatedUser authenticatedUser
						&& jwtTokenProvider.validateToken(jwt, authenticatedUser)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							authenticatedUser, null, authenticatedUser.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch (JwtException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid JWT token",
					request.getRequestURI());
			return;
		} catch (UsernameNotFoundException e) {
			log.warn("User not found for JWT: {}", e.getMessage());
			writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User not found",
					request.getRequestURI());
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String error, String message,
			String path) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ErrorDto errorDto = ErrorDto.of(status.value(), error, message, path);
		response.getWriter().write(objectMapper.writeValueAsString(errorDto));
	}
}
