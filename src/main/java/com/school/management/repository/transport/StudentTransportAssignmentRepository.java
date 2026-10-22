package com.school.management.repository.transport;

import com.school.management.entity.transport.StudentTransportAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentTransportAssignmentRepository extends JpaRepository<StudentTransportAssignmentEntity, Long> {
    List<StudentTransportAssignmentEntity> findByStudentIdOrAdmissionNoIgnoreCaseOrderByIdDesc(Long studentId, String admissionNo);
    List<StudentTransportAssignmentEntity> findBySchoolId(String schoolId);
    Optional<StudentTransportAssignmentEntity> findByStudentIdAndStatus(Long studentId, String status);
    Optional<StudentTransportAssignmentEntity> findFirstByStudentIdOrderByIdDesc(Long studentId);
    List<StudentTransportAssignmentEntity> findByRouteId(Long routeId);
    List<StudentTransportAssignmentEntity> findByPickupStopIdOrDropStopId(Long pickupStopId, Long dropStopId);
    long countByRouteId(Long routeId);
}
