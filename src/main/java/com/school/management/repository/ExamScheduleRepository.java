package com.school.management.repository;

import com.school.management.entity.ExamScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamScheduleEntity, Long> {

    boolean existsByClassNameAndSubject(String className, String subject);

    List<ExamScheduleEntity> findAllByOrderByIdDesc();

    List<ExamScheduleEntity> findByClassNameIgnoreCaseOrderByIdDesc(String className);
}