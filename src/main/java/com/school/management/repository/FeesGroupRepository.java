package com.school.management.repository;

import com.school.management.entity.FeesGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeesGroupRepository extends JpaRepository<FeesGroupEntity, Long> {

    boolean existsByName(String name);
}