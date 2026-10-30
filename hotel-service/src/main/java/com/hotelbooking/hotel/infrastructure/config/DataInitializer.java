package com.hotelbooking.hotel.infrastructure.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.hotelbooking.hotel.domain.entity.Hotel;
import com.hotelbooking.hotel.domain.entity.Room;
import com.hotelbooking.hotel.infrastructure.persistence.HotelRepository;
import com.hotelbooking.hotel.infrastructure.persistence.RoomRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@Profile("dev")
public class DataInitializer {
	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	@Bean
	public CommandLineRunner initData(HotelRepository hotelRepository, RoomRepository roomRepository) {
		return args -> {
			if (hotelRepository.count() > 0) {
				return;
			}
			Hotel hotel1 = hotelRepository.save(new Hotel("Grand Plaza", "123 Main St, Cityville"));
			Hotel hotel2 = hotelRepository.save(new Hotel("Seaside Resort", "456 Ocean Ave, Beachtown"));
			Hotel hotel3 = hotelRepository.save(new Hotel("Empty Inn", "789 Nowhere Rd"));

			Room room101 = new Room(hotel1.getId(), "101");
			for (int i = 0; i < 5; i++) {
				room101.incrementBookings();
			}
			roomRepository.save(room101);

			Room room102 = new Room(hotel1.getId(), "102");
			for (int i = 0; i < 3; i++) {
				room102.incrementBookings();
			}
			roomRepository.save(room102);

			Room room201 = new Room(hotel1.getId(), "201");
			roomRepository.save(room201);

			Room room103 = new Room(hotel2.getId(), "101");
			for (int i = 0; i < 2; i++) {
				room103.incrementBookings();
			}
			roomRepository.save(room103);

			Room room104 = new Room(hotel2.getId(), "102");
			for (int i = 0; i < 7; i++) {
				room104.incrementBookings();
			}
			roomRepository.save(room104);

			Room room105 = new Room(hotel1.getId(), "105");
			room105.update("105", false);
			roomRepository.save(room105);

			log.info("Initialized {} hotels and {} rooms", hotelRepository.count(), roomRepository.count());
		};
	}
}
