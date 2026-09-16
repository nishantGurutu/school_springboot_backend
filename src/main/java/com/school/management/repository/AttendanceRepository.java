package com.school.management.repository;

import com.school.management.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    List<AttendanceEntity> findByAttendanceType(String type);

    List<AttendanceEntity> findByAttendanceTypeAndAttendanceDate(String type, String date);

    List<AttendanceEntity> findByAttendanceTypeAndClassName(String type, String className);

    long countByAttendanceTypeAndAttendanceDateAndStatus(String type, String date, String status);
}