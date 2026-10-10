package com.school.management.repository;

import com.school.management.entity.SubjectChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectChapterRepository extends JpaRepository<SubjectChapterEntity, Long> {

    List<SubjectChapterEntity> findBySubjectIdOrderByDisplayOrderAsc(Long subjectId);

    List<SubjectChapterEntity> findBySubjectIdAndClassNameIgnoreCaseOrderByDisplayOrderAsc(Long subjectId, String className);

    List<SubjectChapterEntity> findByClassNameIgnoreCase(String className);

    List<SubjectChapterEntity> findByClassNameIgnoreCaseOrderByDisplayOrderAsc(String className);
}
