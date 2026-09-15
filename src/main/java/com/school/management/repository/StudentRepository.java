package com.school.management.repository;

import com.school.management.entity.StudentEntity;
import com.school.management.domain.student.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    Optional<StudentEntity> findByAdmissionNo(String admissionNo);

    Optional<StudentEntity> findByEmail(String email);

    List<StudentEntity> findAllByAdmissionNoIn(List<String> admissionNos);

    boolean existsByAdmissionNo(String admissionNo);

    boolean existsByEmail(String email);

    boolean existsByRollNoAndClassName(String rollNo, String className);

    List<StudentEntity> findByClassNameAndStatus(String className, StudentStatus status);

    Page<StudentEntity> findByClassName(String className, Pageable pageable);

    Page<StudentEntity> findByStatus(StudentStatus status, Pageable pageable);

    long countByClassName(String className);

    long countByStatus(StudentStatus status);

    @Query("SELECT s FROM StudentEntity s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.admissionNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.rollNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.guardianName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.className) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<StudentEntity> search(@Param("search") String search, Pageable pageable);
}