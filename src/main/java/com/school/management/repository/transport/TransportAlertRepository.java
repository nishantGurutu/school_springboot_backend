package com.school.management.repository.transport;

import com.school.management.entity.transport.TransportAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportAlertRepository extends JpaRepository<TransportAlertEntity, Long> {
    List<TransportAlertEntity> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
    List<TransportAlertEntity> findBySchoolIdAndStatusOrderByCreatedAtDesc(String schoolId, String status);
    List<TransportAlertEntity> findByTripIdOrderByCreatedAtDesc(Long tripId);
    long countBySchoolIdAndStatus(String schoolId, String status);
}
