package com.school.management.repository;

import com.school.management.entity.LeaveRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

    boolean existsByNameAndLeaveType(String name, String leaveType);

    List<LeaveRequestEntity> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<LeaveRequestEntity> findByUserTypeIgnoreCaseOrderByIdDesc(String userType);

    List<LeaveRequestEntity> findByStatusIgnoreCaseOrderByIdDesc(String status);

    List<LeaveRequestEntity> findAllByOrderByIdDesc();
}
