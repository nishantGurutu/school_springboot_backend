package com.school.management.repository;

import com.school.management.entity.OnlineExamEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OnlineExamRepository extends JpaRepository<OnlineExamEntity,Long> {
 List<OnlineExamEntity> findAllByOrderByIdDesc();
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from OnlineExamEntity e where e.id=:id") Optional<OnlineExamEntity> lockById(@Param("id") Long id);
}
