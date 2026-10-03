package com.school.management.repository;

import com.school.management.entity.FeeCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeCollectionRepository extends JpaRepository<FeeCollectionEntity, Long> {

    boolean existsByAdmissionNo(String admissionNo);

    List<FeeCollectionEntity> findByAdmissionNoIgnoreCaseOrderByIdDesc(String admissionNo);

    List<FeeCollectionEntity> findByClassNameIgnoreCaseOrderByIdDesc(String className);

    List<FeeCollectionEntity> findByStatusIgnoreCaseOrderByIdDesc(String status);

    List<FeeCollectionEntity> findAllByOrderByIdDesc();
}