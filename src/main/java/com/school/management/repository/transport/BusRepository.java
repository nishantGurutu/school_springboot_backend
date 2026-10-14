package com.school.management.repository.transport;

import com.school.management.entity.transport.BusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<BusEntity, Long> {
    List<BusEntity> findBySchoolId(String schoolId);
    Optional<BusEntity> findByIdAndSchoolId(Long id, String schoolId);
    Optional<BusEntity> findByRegistrationNumberIgnoreCase(String registrationNumber);
    List<BusEntity> findBySchoolIdAndStatus(String schoolId, String status);
    long countBySchoolId(String schoolId);
    long countBySchoolIdAndStatus(String schoolId, String status);
}
