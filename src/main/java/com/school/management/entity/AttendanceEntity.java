package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance", indexes = {
        @Index(name = "idx_attendance_type", columnList = "attendanceType"),
        @Index(name = "idx_attendance_date", columnList = "attendanceDate"),
        @Index(name = "idx_attendance_class_name", columnList = "className"),
        @Index(name = "idx_attendance_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 16)
    private String attendanceType;

    @Column(length = 32)
    private String admissionNo;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 16)
    private String rollNo;

    @Column(length = 32)
    private String className;

    @Column(length = 64)
    private String department;

    @Column(length = 64)
    private String designation;

    @Column(length = 32)
    private String attendanceDate;

    @Column(length = 16)
    private String status;

    @Column(length = 255)
    private String note;

    @Column(length = 255)
    private String avatar;

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