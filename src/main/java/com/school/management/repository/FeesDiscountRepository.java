package com.school.management.repository;

import com.school.management.entity.FeesDiscountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeesDiscountRepository extends JpaRepository<FeesDiscountEntity, Long> {

    boolean existsByName(String name);
}