package com.hotelbooking.hotel.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.hotel.domain.entity.RoomLock;

public interface RoomLockRepository extends JpaRepository<RoomLock, Long> {
	Optional<RoomLock> findByRequestId(String requestId);

	@Query("""
			select case when count(l) > 0 then true else false end
			from RoomLock l
			where l.roomId = :roomId
			and l.startDate < :endDate
			and l.endDate > :startDate
			""")
	boolean existsOverlapping(@Param("roomId") Long roomId, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Query("""
			select l from RoomLock l
			where l.status = com.hotelbooking.hotel.domain.entity.RoomLock.LockStatus.HELD
			and l.expiresAt < :now
			""")
	List<RoomLock> findExpiredHeldLocks(@Param("now") LocalDateTime now);
}
