package com.school.management.entity;

import com.school.management.domain.teacher.JobType;
import com.school.management.domain.teacher.TeacherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers", indexes = {
        @Index(name = "idx_teacher_employee_id", columnList = "employeeId"),
        @Index(name = "idx_teacher_email", columnList = "email"),
        @Index(name = "idx_teacher_department", columnList = "department"),
        @Index(name = "idx_teacher_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String employeeId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, length = 64)
    private String department;

    @Column(nullable = false, length = 64)
    private String subject;

    @Column(length = 128)
    private String qualification;

    @Column(length = 64)
    private String designation;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, unique = true, length = 64)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private LocalDate joiningDate;

    private Integer experienceYears;

    @Column(length = 64)
    private String bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private JobType jobType;

    @Column(length = 255)
    private String avatar;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TeacherStatus status = TeacherStatus.ACTIVE;

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