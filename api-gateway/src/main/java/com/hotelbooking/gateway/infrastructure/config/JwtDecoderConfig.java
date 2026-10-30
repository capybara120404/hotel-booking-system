package com.hotelbooking.gateway.infrastructure.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import io.jsonwebtoken.security.Keys;

@Configuration
public class JwtDecoderConfig {

	private static final int MIN_SECRET_BYTES = 32;

	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${jwt.secret}") String secret) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

		if (keyBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
		}

		SecretKey key = Keys.hmacShaKeyFor(keyBytes);

		return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}
}
