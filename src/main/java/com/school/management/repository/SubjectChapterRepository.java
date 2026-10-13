package com.school.management.repository;

import com.school.management.entity.SubjectChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface SubjectChapterRepository extends JpaRepository<SubjectChapterEntity, Long> {

    List<SubjectChapterEntity> findBySubjectIdOrderByDisplayOrderAsc(Long subjectId);

    List<SubjectChapterEntity> findBySubjectIdAndClassNameIgnoreCaseOrderByDisplayOrderAsc(Long subjectId, String className);

    @Query("SELECT c FROM SubjectChapterEntity c WHERE c.subject.id = :subjectId AND (LOWER(TRIM(c.className)) = LOWER(TRIM(:className)) OR LOWER(TRIM(c.className)) = 'all' OR c.className IS NULL) ORDER BY c.displayOrder ASC")
    List<SubjectChapterEntity> findBySubjectIdAndClassNameFlexible(@Param("subjectId") Long subjectId, @Param("className") String className);

    List<SubjectChapterEntity> findByClassNameIgnoreCase(String className);

    List<SubjectChapterEntity> findByClassNameIgnoreCaseOrderByDisplayOrderAsc(String className);

    @Query("SELECT c FROM SubjectChapterEntity c WHERE (LOWER(TRIM(c.className)) = LOWER(TRIM(:className)) OR LOWER(TRIM(c.className)) = 'all' OR c.className IS NULL) ORDER BY c.displayOrder ASC")
    List<SubjectChapterEntity> findByClassNameFlexible(@Param("className") String className);
}

