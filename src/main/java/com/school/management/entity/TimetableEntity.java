package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "timetables", indexes = {
        @Index(name = "idx_timetable_class", columnList = "className"),
        @Index(name = "idx_timetable_section", columnList = "section"),
        @Index(name = "idx_timetable_day", columnList = "dayOfWeek"),
        @Index(name = "idx_timetable_teacher", columnList = "teacherName"),
        @Index(name = "idx_timetable_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String className;

    @Column(nullable = false, length = 32)
    private String section;

    @Column(nullable = false, length = 32)
    private String dayOfWeek;

    @Column(length = 64)
    private String periodName;

    @Column(nullable = false, length = 128)
    private String subject;

    private Long teacherId;

    @Column(length = 128)
    private String teacherName;

    @Column(length = 64)
    private String classroom;

    @Column(nullable = false, length = 32)
    private String startTime;

    @Column(nullable = false, length = 32)
    private String endTime;

    @Builder.Default
    @Column(nullable = false, length = 16)
    private String status = "Active";

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
