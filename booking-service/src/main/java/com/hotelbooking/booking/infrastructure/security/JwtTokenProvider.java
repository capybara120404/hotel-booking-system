package com.hotelbooking.booking.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {
	private static final int MIN_SECRET_BYTES = 32;
	private static final String USER_ID_CLAIM = "userId";
	private final SecretKey secretKey;
	private final long jwtExpirationMs;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration-ms:3600000}") long jwtExpirationMs) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
		}
		if (jwtExpirationMs <= 0) {
			throw new IllegalStateException("JWT expiration must be positive");
		}
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
		this.jwtExpirationMs = jwtExpirationMs;
	}

	public String generateToken(AuthenticatedUser user) {
		List<String> roles = user.getAuthorities().stream().map(a -> a.getAuthority()).toList();
		return buildToken(user.getUsername(), user.getId(), roles);
	}

	public String generateServiceToken(String subject, List<String> roles) {
		return buildToken(subject, null, roles);
	}

	private String buildToken(String subject, Long userId, List<String> roles) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
		var builder = Jwts.builder().subject(subject).claim("roles", roles).issuedAt(now).expiration(expiryDate);
		if (userId != null) {
			builder.claim(USER_ID_CLAIM, userId);
		}
		return builder.signWith(secretKey, Jwts.SIG.HS256).compact();
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public boolean validateToken(String token, AuthenticatedUser user) {
		String username = extractUsername(token);
		return username != null && username.equals(user.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractClaim(token, Claims::getExpiration).before(new Date());
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		return claimsResolver.apply(extractAllClaims(token));
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
	}
}
