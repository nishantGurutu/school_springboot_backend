package com.school.management.repository;

import com.school.management.entity.HomeworkSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmissionEntity, Long> {
    Optional<HomeworkSubmissionEntity> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);
}
