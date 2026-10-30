package com.hotelbooking.hotel.infrastructure.config;

import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	private static final String[] SWAGGER_ENDPOINTS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs",
			"/v3/api-docs/**"};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/hotels").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/hotels/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/recommend")
						.hasAnyRole("USER", "ADMIN").requestMatchers(HttpMethod.POST, "/api/rooms").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/rooms/hotel/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/rooms/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/rooms/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/rooms/*/confirm-availability", "/api/rooms/*/release",
								"/api/rooms/*/commit")
						.hasRole("SERVICE").anyRequest().authenticated())
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesJwtAuthenticationConverter())));
		return http.build();
	}

	@Bean
	public Converter<Jwt, ? extends AbstractAuthenticationToken> rolesJwtAuthenticationConverter() {
		return jwt -> {
			List<String> roles = jwt.getClaimAsStringList("roles");
			Collection<SimpleGrantedAuthority> authorities = roles == null
					? List.of()
					: roles.stream().map(SimpleGrantedAuthority::new).toList();
			return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
		};
	}
}
