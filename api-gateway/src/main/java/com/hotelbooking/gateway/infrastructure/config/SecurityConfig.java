package com.hotelbooking.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {"/user/register", "/user/auth", "/swagger-ui.html",
			"/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",};

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		http.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange.pathMatchers(PUBLIC_ENDPOINTS).permitAll()
						.pathMatchers(HttpMethod.GET, "/api/hotels", "/api/hotels/**", "/api/rooms", "/api/rooms/**")
						.permitAll().anyExchange().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

		return http.build();
	}
}
