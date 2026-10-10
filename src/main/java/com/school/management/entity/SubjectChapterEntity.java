package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subject_chapters", indexes = {
        @Index(name = "idx_chapter_subject", columnList = "subject_id"),
        @Index(name = "idx_chapter_class", columnList = "class_name"),
        @Index(name = "idx_chapter_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectChapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @Column(name = "class_name", nullable = false, length = 64)
    private String className;

    @Column(name = "chapter_number", nullable = false, length = 32)
    private String chapterNumber;

    @Column(name = "chapter_title", nullable = false, length = 255)
    private String chapterTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (displayOrder == null) displayOrder = 1;
        if (chapterNumber == null || chapterNumber.isBlank()) chapterNumber = "Chapter 1";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
