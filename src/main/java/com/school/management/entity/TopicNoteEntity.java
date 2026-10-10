package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_notes", indexes = {
        @Index(name = "idx_note_topic", columnList = "topic_id"),
        @Index(name = "idx_note_teacher", columnList = "uploaded_by_teacher_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private SubjectChapterEntity chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private SubjectEntity subject;

    @Column(name = "class_name", length = 64)
    private String className;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = true)
    private SubjectTopicEntity topic;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 1024)
    private String fileUrl;

    @Builder.Default
    @Column(name = "file_type", length = 32)
    private String fileType = "PDF"; // PDF, DOC, IMAGE, LINK

    @Builder.Default
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes = 0L;

    @Column(name = "file_size_formatted", length = 32)
    private String fileSizeFormatted;

    @Builder.Default
    @Column(name = "is_downloadable")
    private Boolean isDownloadable = true;

    @Builder.Default
    @Column(name = "is_previewable")
    private Boolean isPreviewable = true;

    @Column(name = "uploaded_by_teacher_id")
    private Long uploadedByTeacherId;

    @Column(name = "uploaded_by_teacher_name", length = 128)
    private String uploadedByTeacherName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fileType == null || fileType.isBlank()) fileType = "PDF";
        if (isDownloadable == null) isDownloadable = true;
        if (isPreviewable == null) isPreviewable = true;
        if (fileSizeBytes == null) fileSizeBytes = 0L;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
