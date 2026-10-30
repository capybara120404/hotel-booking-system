package com.hotelbooking.hotel.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.hotel.domain.entity.RoomLock;
import com.hotelbooking.hotel.infrastructure.persistence.RoomLockRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReconciliationService {
	private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

	private final RoomLockRepository roomLockRepository;

	public ReconciliationService(RoomLockRepository roomLockRepository) {
		this.roomLockRepository = roomLockRepository;
	}

	@Scheduled(fixedDelay = 60000)
	@Transactional
	public void cleanupExpiredLocks() {
		LocalDateTime now = LocalDateTime.now();
		List<RoomLock> expiredLocks = roomLockRepository.findExpiredHeldLocks(now);
		for (RoomLock lock : expiredLocks) {
			log.warn("Cleaning up expired HELD lock for room {} and requestId={}", lock.getRoomId(),
					lock.getRequestId());
			roomLockRepository.delete(lock);
		}
		if (!expiredLocks.isEmpty()) {
			log.info("Cleaned up {} expired HELD room locks", expiredLocks.size());
		}
	}
}
