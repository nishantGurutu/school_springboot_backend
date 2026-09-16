package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "library_members", indexes = {
        @Index(name = "idx_library_member_card_no", columnList = "card_no"),
        @Index(name = "idx_library_member_student_name", columnList = "student_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32)
    private String joinDate;

    @Column(name = "card_no", nullable = false, unique = true, length = 64)
    private String cardNo;

    @Column(name = "student_name", nullable = false, length = 128)
    private String studentName;

    @Column(length = 64)
    private String className;

    @Column(length = 16)
    private String section;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String email;

    @Column(length = 16)
    private String gender;

    @Column(length = 255)
    private String bookIssue;

    @Column(length = 32)
    private String issueDate;

    @Column(length = 32)
    private String returnDate;

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