package com.hotelbooking.booking.infrastructure.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hotelbooking.booking.domain.entity.Role;
import com.hotelbooking.booking.domain.entity.User;
import com.hotelbooking.booking.infrastructure.persistence.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@Profile("dev")
public class DataInitializer {
	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	@Bean
	public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.count() > 0) {
				return;
			}
			userRepository.save(new User("admin", passwordEncoder.encode("admin123"), Role.ADMIN));
			userRepository.save(new User("user", passwordEncoder.encode("user123"), Role.USER));
			log.info("Initialized {} users", userRepository.count());
		};
	}
}
