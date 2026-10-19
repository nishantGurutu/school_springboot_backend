package com.school.management.repository;
import com.school.management.entity.OnlineExamEntity;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface OnlineExamRepository extends JpaRepository<OnlineExamEntity,Long> {
 List<OnlineExamEntity> findAllByOrderByIdDesc();
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from OnlineExamEntity e where e.id=:id") Optional<OnlineExamEntity> lockById(@Param("id") Long id);
}
