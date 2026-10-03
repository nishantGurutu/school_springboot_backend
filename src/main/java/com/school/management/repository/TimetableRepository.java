package com.school.management.repository;

import com.school.management.entity.TimetableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntity, Long>, JpaSpecificationExecutor<TimetableEntity> {

    List<TimetableEntity> findByClassNameIgnoreCaseAndSectionIgnoreCase(String className, String section);

    List<TimetableEntity> findByClassNameIgnoreCase(String className);

    List<TimetableEntity> findByTeacherNameContainingIgnoreCase(String teacherName);

    List<TimetableEntity> findByTeacherId(Long teacherId);

    List<TimetableEntity> findByDayOfWeekIgnoreCase(String dayOfWeek);
}
