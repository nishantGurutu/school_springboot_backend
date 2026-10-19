package com.school.management.repository;

import com.school.management.entity.FeeCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeCollectionRepository extends JpaRepository<FeeCollectionEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select f from FeeCollectionEntity f where f.id=:id")
    java.util.Optional<FeeCollectionEntity> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByAdmissionNo(String admissionNo);

    List<FeeCollectionEntity> findByAdmissionNoIgnoreCaseOrderByIdDesc(String admissionNo);

    List<FeeCollectionEntity> findByClassNameIgnoreCaseOrderByIdDesc(String className);

    List<FeeCollectionEntity> findByStatusIgnoreCaseOrderByIdDesc(String status);

    List<FeeCollectionEntity> findAllByOrderByIdDesc();
}