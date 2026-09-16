package com.school.management.repository;

import com.school.management.entity.ExamResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResultEntity, Long> {

    boolean existsByAdmissionNo(String admissionNo);
}