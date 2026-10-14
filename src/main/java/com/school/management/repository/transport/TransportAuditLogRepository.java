package com.school.management.repository.transport;

import com.school.management.entity.transport.TransportAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportAuditLogRepository extends JpaRepository<TransportAuditLogEntity, Long> {
    List<TransportAuditLogEntity> findBySchoolIdOrderByOccurredAtDesc(String schoolId);
    List<TransportAuditLogEntity> findByResourceTypeAndResourceIdOrderByOccurredAtDesc(String resourceType, Long resourceId);
}
