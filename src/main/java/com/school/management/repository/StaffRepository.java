package com.school.management.repository;

import com.school.management.entity.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<StaffEntity, Long> {

    Optional<StaffEntity> findByEmail(String email);
    Optional<StaffEntity> findByEmailIgnoreCase(String email);
    Optional<StaffEntity> findByPhone(String phone);
    boolean existsByEmail(String email);
}