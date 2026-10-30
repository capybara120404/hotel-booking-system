package com.hotelbooking.hotel.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.hotel.domain.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
	boolean existsByHotelId(Long hotelId);
	Page<Room> findByHotelId(Long hotelId, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.id = :id")
	Optional<Room> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			    select r from Room r
			    where r.available = true
			    and r.id not in (
			        select l.roomId from RoomLock l
			        where l.roomId = r.id
			        and l.startDate < :endDate
			        and l.endDate > :startDate
			    )
			""")
	Page<Room> findFreeRooms(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
			Pageable pageable);

	@Query("""
			    select r from Room r
			    where r.available = true
			    and r.id not in (
			        select l.roomId from RoomLock l
			        where l.roomId = r.id
			        and l.startDate < :endDate
			        and l.endDate > :startDate
			    )
			    order by r.timesBooked asc, r.id asc
			""")
	Page<Room> findRecommendedRooms(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
			Pageable pageable);

	@Query("""
			    select r.hotelId, h.name, count(r), coalesce(sum(r.timesBooked), 0)
			    from Room r join Hotel h on r.hotelId = h.id
			    group by r.hotelId, h.name
			""")
	List<Object[]> findStatsByHotel();

	@Modifying
	@Query("update Room r set r.timesBooked = r.timesBooked + 1 where r.id = :roomId")
	void incrementTimesBooked(@Param("roomId") Long roomId);

	@Modifying
	@Query("update Room r set r.timesBooked = r.timesBooked - 1 where r.id = :roomId and r.timesBooked > 0")
	void decrementTimesBooked(@Param("roomId") Long roomId);
}
