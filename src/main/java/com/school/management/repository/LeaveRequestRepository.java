package com.school.management.repository;

import com.school.management.entity.LeaveRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

    boolean existsByNameAndLeaveType(String name, String leaveType);
}
