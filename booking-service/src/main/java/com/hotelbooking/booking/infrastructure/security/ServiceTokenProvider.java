package com.hotelbooking.booking.infrastructure.security;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ServiceTokenProvider {

	private static final String SERVICE_SUBJECT = "service";
	private static final List<String> SERVICE_ROLES = List.of("ROLE_SERVICE");
	private final JwtTokenProvider jwtTokenProvider;

	public ServiceTokenProvider(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	public String getServiceToken() {
		return jwtTokenProvider.generateServiceToken(SERVICE_SUBJECT, SERVICE_ROLES);
	}
}
