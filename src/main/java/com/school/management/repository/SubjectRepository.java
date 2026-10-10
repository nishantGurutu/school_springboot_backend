package com.school.management.repository;

import com.school.management.entity.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {

    boolean existsByCode(String code);

    java.util.List<SubjectEntity> findByNameIgnoreCase(String name);

    java.util.List<SubjectEntity> findByClassNameIgnoreCase(String className);

    java.util.List<SubjectEntity> findByClassNameIgnoreCaseOrClassNameIsNull(String className);
}
