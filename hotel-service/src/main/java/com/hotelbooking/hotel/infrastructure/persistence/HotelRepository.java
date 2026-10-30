package com.hotelbooking.hotel.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelbooking.hotel.domain.entity.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
	Page<Hotel> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
