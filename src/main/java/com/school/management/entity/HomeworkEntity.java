package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "homeworks", indexes = {
        @Index(name = "idx_homework_class", columnList = "className"),
        @Index(name = "idx_homework_subject", columnList = "subject"),
        @Index(name = "idx_homework_due_date", columnList = "dueDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String className;

    private String section;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String assignedDate;

    private String dueDate;

    @Builder.Default
    private String status = "ACTIVE";

    private String assignedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
