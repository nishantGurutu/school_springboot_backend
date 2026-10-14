package com.school.management.repository.transport;

import com.school.management.entity.transport.TransportRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportRouteRepository extends JpaRepository<TransportRouteEntity, Long> {
    List<TransportRouteEntity> findBySchoolId(String schoolId);
    Optional<TransportRouteEntity> findByIdAndSchoolId(Long id, String schoolId);
    Optional<TransportRouteEntity> findByRouteCodeIgnoreCase(String routeCode);
    List<TransportRouteEntity> findBySchoolIdAndStatus(String schoolId, String status);
    long countBySchoolId(String schoolId);
}
