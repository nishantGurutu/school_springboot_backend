package com.school.management.repository.transport;

import com.school.management.entity.transport.BusLocationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusLocationHistoryRepository extends JpaRepository<BusLocationHistoryEntity, Long> {
    List<BusLocationHistoryEntity> findByTripIdOrderByRecordedAtAsc(Long tripId);
    List<BusLocationHistoryEntity> findByBusIdOrderByRecordedAtDesc(Long busId);
    void deleteByTripId(Long tripId);
}
