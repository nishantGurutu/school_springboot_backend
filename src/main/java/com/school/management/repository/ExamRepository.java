package com.school.management.repository;

import com.school.management.entity.ExamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<ExamEntity, Long> {

    boolean existsByName(String name);

    List<ExamEntity> findAllByOrderByIdDesc();
}