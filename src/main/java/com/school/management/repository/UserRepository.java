package com.school.management.repository;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    java.util.List<UserEntity> findBySchoolIdAndEnabledTrue(String schoolId);
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
