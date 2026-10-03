package com.school.management.repository;

import com.school.management.entity.ExamResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResultEntity, Long> {

    boolean existsByAdmissionNo(String admissionNo);

    List<ExamResultEntity> findByAdmissionNoIgnoreCaseOrderByIdDesc(String admissionNo);

    List<ExamResultEntity> findByClassNameIgnoreCaseOrderByIdDesc(String className);

    List<ExamResultEntity> findByExamContainingIgnoreCaseOrderByIdDesc(String exam);

    Optional<ExamResultEntity> findFirstByAdmissionNoIgnoreCaseAndExamIgnoreCase(String admissionNo, String exam);

    List<ExamResultEntity> findAllByOrderByIdDesc();
}