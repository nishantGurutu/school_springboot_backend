package com.school.management.repository.transport;

import com.school.management.entity.transport.TransportTripEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransportTripRepository extends JpaRepository<TransportTripEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransportTripEntity t where t.id = :id")
    Optional<TransportTripEntity> findForCompletion(@Param("id") Long id);

    Optional<TransportTripEntity> findFirstByRouteIdAndSchoolIdAndTripDateOrderByIdDesc(Long routeId, String schoolId, LocalDate tripDate);
    List<TransportTripEntity> findBySchoolId(String schoolId);
    Optional<TransportTripEntity> findByIdAndSchoolId(Long id, String schoolId);
    List<TransportTripEntity> findBySchoolIdAndStatus(String schoolId, String status);
    List<TransportTripEntity> findBySchoolIdAndTripDate(String schoolId, LocalDate tripDate);
    Optional<TransportTripEntity> findFirstByDriverIdAndStatus(Long driverId, String status);
    Optional<TransportTripEntity> findFirstByDriverIdAndTripDateAndStatus(Long driverId, LocalDate tripDate, String status);
    Optional<TransportTripEntity> findFirstByRouteIdAndSchoolIdAndStatus(Long routeId, String schoolId, String status);
    Optional<TransportTripEntity> findFirstByBusIdAndStatus(Long busId, String status);
    List<TransportTripEntity> findByDriverIdOrderByTripDateDesc(Long driverId);
    long countBySchoolIdAndStatus(String schoolId, String status);
}
