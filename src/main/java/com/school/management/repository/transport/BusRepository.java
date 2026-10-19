package com.school.management.repository.transport;

import com.school.management.entity.transport.BusEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<BusEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BusEntity b where b.id = :id and b.schoolId = :schoolId")
    Optional<BusEntity> findForDriverAssignment(@Param("id") Long id, @Param("schoolId") String schoolId);

    @Query("select b from BusEntity b where b.schoolId = :schoolId and b.status = 'ACTIVE' " +
           "and not exists (select d.id from DriverProfileEntity d where d.assignedBusId = b.id) " +
           "and not exists (select a.id from BusAssignmentEntity a where a.busId = b.id and a.status = 'ACTIVE') " +
           "and not exists (select t.id from TransportTripEntity t where t.busId = b.id and t.status = 'IN_PROGRESS') " +
           "order by b.busNumber")
    List<BusEntity> findAvailableForDriver(@Param("schoolId") String schoolId);

    List<BusEntity> findBySchoolId(String schoolId);
    Optional<BusEntity> findByIdAndSchoolId(Long id, String schoolId);
    Optional<BusEntity> findByRegistrationNumberIgnoreCase(String registrationNumber);
    List<BusEntity> findBySchoolIdAndStatus(String schoolId, String status);
    long countBySchoolId(String schoolId);
    long countBySchoolIdAndStatus(String schoolId, String status);
}
