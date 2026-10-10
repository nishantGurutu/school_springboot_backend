package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subject_topics", indexes = {
        @Index(name = "idx_topic_chapter", columnList = "chapter_id"),
        @Index(name = "idx_topic_subject", columnList = "subject_id"),
        @Index(name = "idx_topic_class", columnList = "class_name"),
        @Index(name = "idx_topic_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectTopicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private SubjectChapterEntity chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectEntity subject;

    @Column(name = "class_name", nullable = false, length = 64)
    private String className;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(length = 32)
    private String difficulty = "Medium"; // Easy, Medium, Hard

    @Builder.Default
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes = 30;

    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Builder.Default
    @Column(length = 32)
    private String status = "Active";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (displayOrder == null) displayOrder = 1;
        if (difficulty == null || difficulty.isBlank()) difficulty = "Medium";
        if (estimatedMinutes == null) estimatedMinutes = 30;
        if (status == null || status.isBlank()) status = "Active";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
