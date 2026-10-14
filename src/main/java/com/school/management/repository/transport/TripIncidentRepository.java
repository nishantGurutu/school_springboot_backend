package com.school.management.repository.transport;

import com.school.management.entity.transport.TripIncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripIncidentRepository extends JpaRepository<TripIncidentEntity, Long> {
    List<TripIncidentEntity> findBySchoolId(String schoolId);
    List<TripIncidentEntity> findByTripId(Long tripId);
    List<TripIncidentEntity> findBySchoolIdAndStatus(String schoolId, String status);
    List<TripIncidentEntity> findByDriverId(Long driverId);
}
