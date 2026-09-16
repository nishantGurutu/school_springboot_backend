package com.school.management.repository;

import com.school.management.entity.FeeCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeeCollectionRepository extends JpaRepository<FeeCollectionEntity, Long> {

    boolean existsByAdmissionNo(String admissionNo);
}