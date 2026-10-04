package com.school.management.repository;

import com.school.management.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    Optional<DepartmentEntity> findByCodeIgnoreCase(String code);

    List<DepartmentEntity> findAllByOrderByIdAsc();
}
