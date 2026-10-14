package com.school.management.repository.transport;

import com.school.management.entity.transport.TripStopEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripStopEventRepository extends JpaRepository<TripStopEventEntity, Long> {
    List<TripStopEventEntity> findByTripIdOrderByStopOrderAsc(Long tripId);
    Optional<TripStopEventEntity> findByTripIdAndStopId(Long tripId, Long stopId);
}
