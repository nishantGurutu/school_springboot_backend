package com.school.management.repository;

import com.school.management.entity.TeacherEntity;
import com.school.management.domain.teacher.TeacherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {

    Optional<TeacherEntity> findByEmployeeId(String employeeId);

    Optional<TeacherEntity> findByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    List<TeacherEntity> findByDepartmentAndStatus(String department, TeacherStatus status);

    Page<TeacherEntity> findByDepartment(String department, Pageable pageable);

    Page<TeacherEntity> findByStatus(TeacherStatus status, Pageable pageable);

    long countByStatus(TeacherStatus status);

    @Query("SELECT t FROM TeacherEntity t WHERE " +
            "LOWER(t.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.employeeId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TeacherEntity> search(@Param("search") String search, Pageable pageable);
}