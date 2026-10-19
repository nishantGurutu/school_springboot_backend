package com.school.management.repository.transport;

import com.school.management.entity.transport.DriverProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfileEntity, Long> {
    List<DriverProfileEntity> findByAssignedBusId(Long busId);
    List<DriverProfileEntity> findBySchoolId(String schoolId);
    Optional<DriverProfileEntity> findByIdAndSchoolId(Long id, String schoolId);
    Optional<DriverProfileEntity> findByEmailIgnoreCase(String email);
    Optional<DriverProfileEntity> findByPhone(String phone);
    Optional<DriverProfileEntity> findByLicenseNumberIgnoreCase(String licenseNumber);
    Optional<DriverProfileEntity> findByUserId(Long userId);
    List<DriverProfileEntity> findBySchoolIdAndStatus(String schoolId, String status);
    long countBySchoolId(String schoolId);
}
