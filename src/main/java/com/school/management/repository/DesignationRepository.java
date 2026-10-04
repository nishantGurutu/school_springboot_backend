package com.school.management.repository;

import com.school.management.entity.DesignationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignationRepository extends JpaRepository<DesignationEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<DesignationEntity> findAllByOrderByIdAsc();

    List<DesignationEntity> findByStatusIgnoreCaseOrderByIdAsc(String status);
}
