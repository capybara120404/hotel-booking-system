package com.hotelbooking.gateway.infrastructure.filter;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class InternalRouteBlockFilter implements GlobalFilter, Ordered {
	private static final Set<String> INTERNAL_SUFFIXES = Set.of("/confirm-availability", "/release",
			"/increment-booking-count", "/decrement-booking-count");
	private static final Pattern INTERNAL_ROOM_PATTERN = Pattern
			.compile(".*/\\d+(?:" + String.join("|", INTERNAL_SUFFIXES) + ")(\\?.*)?");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		if (isInternalRoomOperation(path)) {
			exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
			return exchange.getResponse().setComplete();
		}
		return chain.filter(exchange);
	}

	private boolean isInternalRoomOperation(String path) {
		return path.startsWith("/api/rooms/") && INTERNAL_ROOM_PATTERN.matcher(path).matches();
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
