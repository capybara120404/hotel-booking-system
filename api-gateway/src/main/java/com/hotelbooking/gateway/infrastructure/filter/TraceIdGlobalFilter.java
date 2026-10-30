package com.hotelbooking.gateway.infrastructure.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.hotelbooking.gateway.infrastructure.config.MdcThreadLocalAccessor;

import reactor.core.publisher.Mono;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {
	private static final String TRACE_ID_HEADER = "X-Request-Id";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString();
		} else {
			traceId = traceId.replaceAll("[^a-zA-Z0-9\\-]", "");
			if (traceId.isBlank()) {
				traceId = UUID.randomUUID().toString();
			}
		}
		final String finalTraceId = traceId;
		ServerHttpRequest mutatedRequest = exchange.getRequest().mutate().header(TRACE_ID_HEADER, finalTraceId).build();
		return chain.filter(exchange.mutate().request(mutatedRequest).build())
				.contextWrite(ctx -> ctx.put(MdcThreadLocalAccessor.TRACE_ID_KEY, finalTraceId));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}
}
