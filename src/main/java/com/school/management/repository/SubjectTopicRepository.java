package com.school.management.repository;

import com.school.management.entity.SubjectTopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectTopicRepository extends JpaRepository<SubjectTopicEntity, Long> {

    List<SubjectTopicEntity> findByChapterIdOrderByDisplayOrderAsc(Long chapterId);

    List<SubjectTopicEntity> findBySubjectIdOrderByDisplayOrderAsc(Long subjectId);

    List<SubjectTopicEntity> findBySubjectIdAndClassNameIgnoreCaseOrderByDisplayOrderAsc(Long subjectId, String className);
}
