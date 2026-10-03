package com.school.management.repository;

import com.school.management.entity.HomeworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkRepository extends JpaRepository<HomeworkEntity, Long> {

    List<HomeworkEntity> findByClassNameIgnoreCaseOrderByIdDesc(String className);

    List<HomeworkEntity> findByClassNameIgnoreCaseAndSectionIgnoreCaseOrderByIdDesc(String className, String section);

    List<HomeworkEntity> findAllByOrderByIdDesc();
}
