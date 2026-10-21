package com.school.management.repository.transport;

import com.school.management.entity.transport.BusAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusAssignmentRepository extends JpaRepository<BusAssignmentEntity, Long> {
    List<BusAssignmentEntity> findBySchoolId(String schoolId);
    Optional<BusAssignmentEntity> findByIdAndSchoolId(Long id, String schoolId);
    List<BusAssignmentEntity> findByDriverId(Long driverId);
    List<BusAssignmentEntity> findByDriverIdAndStatus(Long driverId, String status);
    Optional<BusAssignmentEntity> findFirstByDriverIdAndStatusOrderByIdDesc(Long driverId, String status);
    List<BusAssignmentEntity> findByBusIdAndStatus(Long busId, String status);
    List<BusAssignmentEntity> findByRouteIdAndStatus(Long routeId, String status);
    void deleteByDriverId(Long driverId);
}

