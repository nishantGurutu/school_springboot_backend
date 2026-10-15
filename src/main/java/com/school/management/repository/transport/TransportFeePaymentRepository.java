package com.school.management.repository.transport;

import com.school.management.entity.transport.TransportFeePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportFeePaymentRepository extends JpaRepository<TransportFeePaymentEntity, Long> {

    List<TransportFeePaymentEntity> findBySchoolIdOrderByPaymentDateDesc(String schoolId);

    List<TransportFeePaymentEntity> findBySchoolIdAndStudentId(String schoolId, Long studentId);

    List<TransportFeePaymentEntity> findByAssignmentId(Long assignmentId);
}
