package com.school.management.entity;

import com.school.management.domain.guardian.GuardianType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guardians", indexes = {
        @Index(name = "idx_guardian_email", columnList = "email"),
        @Index(name = "idx_guardian_student", columnList = "student_admission_no"),
        @Index(name = "idx_guardian_type", columnList = "guardian_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private GuardianType guardianType;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String occupation;

    @Column(unique = true, length = 64)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String photo;

    @Builder.Default
    @Column(length = 20)
    private String feeStatus = "Clear";

    // Reference to the student this parent/guardian belongs to
    @Column(length = 32)
    private String studentAdmissionNo;

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