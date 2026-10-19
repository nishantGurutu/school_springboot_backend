package com.school.management.repository;
import com.school.management.entity.FeePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FeePaymentRepository extends JpaRepository<FeePaymentEntity,Long> {
 List<FeePaymentEntity> findByFeeIdOrderByIdDesc(Long feeId);
 List<FeePaymentEntity> findTop50ByStatusOrderByIdAsc(String status);
}
