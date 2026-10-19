package com.school.management.repository;
import com.school.management.entity.OnlineExamAttemptEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface OnlineExamAttemptRepository extends JpaRepository<OnlineExamAttemptEntity,Long> {
 Optional<OnlineExamAttemptEntity> findByExamIdAndStudentId(Long examId,Long studentId);
 List<OnlineExamAttemptEntity> findByExamId(Long examId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from OnlineExamAttemptEntity a where a.id=:id") Optional<OnlineExamAttemptEntity> lockById(@Param("id") Long id);
}
