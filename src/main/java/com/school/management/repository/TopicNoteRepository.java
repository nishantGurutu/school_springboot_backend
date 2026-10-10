package com.school.management.repository;

import com.school.management.entity.TopicNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicNoteRepository extends JpaRepository<TopicNoteEntity, Long> {

    List<TopicNoteEntity> findByTopicIdOrderByCreatedAtDesc(Long topicId);

    long countByTopicId(Long topicId);

    List<TopicNoteEntity> findByChapterIdOrderByCreatedAtDesc(Long chapterId);

    long countByChapterId(Long chapterId);

    List<TopicNoteEntity> findBySubjectIdOrderByCreatedAtDesc(Long subjectId);

    List<TopicNoteEntity> findByUploadedByTeacherId(Long teacherId);
}
