package com.school.management.repository;

import com.school.management.entity.GuardianEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianRepository extends JpaRepository<GuardianEntity, Long> {

    Optional<GuardianEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<GuardianEntity> findByStudentAdmissionNo(String studentAdmissionNo);

    Page<GuardianEntity> findByGuardianType(com.school.management.domain.guardian.GuardianType type, Pageable pageable);

    long countByGuardianType(com.school.management.domain.guardian.GuardianType type);

    @Query("SELECT g FROM GuardianEntity g WHERE " +
            "LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(g.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(g.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(g.occupation) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<GuardianEntity> search(@Param("search") String search, Pageable pageable);
}