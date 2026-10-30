package com.hotelbooking.booking.infrastructure.security;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.MDC;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
	private static final String TRACE_ID_HEADER = "X-Request-Id";
	private static final String TRACE_ID_MDC_KEY = "traceId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = request.getHeader(TRACE_ID_HEADER);
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString();
		} else {
			traceId = traceId.replaceAll("[^a-zA-Z0-9\\-]", "");
			if (traceId.isBlank()) {
				traceId = UUID.randomUUID().toString();
			}
		}
		MDC.put(TRACE_ID_MDC_KEY, traceId);
		try {
			response.setHeader(TRACE_ID_HEADER, traceId);
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID_MDC_KEY);
		}
	}
}
