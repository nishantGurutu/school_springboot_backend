package com.school.management.repository.transport;

import com.school.management.entity.transport.TransportTripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransportTripRepository extends JpaRepository<TransportTripEntity, Long> {
    List<TransportTripEntity> findBySchoolId(String schoolId);
    Optional<TransportTripEntity> findByIdAndSchoolId(Long id, String schoolId);
    List<TransportTripEntity> findBySchoolIdAndStatus(String schoolId, String status);
    List<TransportTripEntity> findBySchoolIdAndTripDate(String schoolId, LocalDate tripDate);
    Optional<TransportTripEntity> findFirstByDriverIdAndStatus(Long driverId, String status);
    Optional<TransportTripEntity> findFirstByDriverIdAndTripDateAndStatus(Long driverId, LocalDate tripDate, String status);
    Optional<TransportTripEntity> findFirstByBusIdAndStatus(Long busId, String status);
    List<TransportTripEntity> findByDriverIdOrderByTripDateDesc(Long driverId);
    long countBySchoolIdAndStatus(String schoolId, String status);
}
