package com.hotelbooking.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hotelbooking.gateway.infrastructure.config.MdcThreadLocalAccessor;

import io.micrometer.context.ContextRegistry;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		ContextRegistry.getInstance().registerThreadLocalAccessor(new MdcThreadLocalAccessor());
		Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
