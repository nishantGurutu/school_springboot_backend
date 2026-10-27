package com.school.management.repository;
import com.school.management.entity.StudentReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StudentReportRepository extends JpaRepository<StudentReportEntity,Long> {
    List<StudentReportEntity> findByStudentIdOrderByIdDesc(Long studentId);
    List<StudentReportEntity> findAllByOrderByIdDesc();
}
