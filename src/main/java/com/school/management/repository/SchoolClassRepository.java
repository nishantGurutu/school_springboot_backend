package com.school.management.repository;

import com.school.management.entity.SchoolClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClassEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndSectionId(String name, Long sectionId);

    boolean existsByNameAndSection(String name, String section);

    Optional<SchoolClassEntity> findByNameAndSectionId(String name, Long sectionId);

    Optional<SchoolClassEntity> findByNameAndSection(String name, String section);
}
