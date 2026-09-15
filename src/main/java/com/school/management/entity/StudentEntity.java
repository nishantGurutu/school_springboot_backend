package com.school.management.entity;

import com.school.management.domain.student.Gender;
import com.school.management.domain.student.StudentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_admission_no", columnList = "admissionNo"),
        @Index(name = "idx_student_email", columnList = "email"),
        @Index(name = "idx_student_class", columnList = "className"),
        @Index(name = "idx_student_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String admissionNo;

    // Full name as shown in the dashboard ("Alexander Wright")
    @Column(length = 128)
    private String name;

    // Derived from full name; kept populated for backward schema compatibility
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, length = 20)
    private String rollNo;

    // Dashboard "Class" select (e.g. "Grade 10", "Primary")
    @Column(nullable = false, length = 32)
    private String className;

    @Column(nullable = false, length = 32)
    private String section;

    @Column(length = 32)
    private String category;

    @Column(length = 32)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 64)
    private String email;

    @Column(length = 255)
    private String studentPhoto;

    // --- Parent & Guardian Info ---
    @Column(length = 64)
    private String fatherName;

    @Column(length = 20)
    private String fatherPhone;

    @Column(length = 64)
    private String fatherOccupation;

    @Column(length = 255)
    private String fatherPhoto;

    @Column(length = 64)
    private String motherName;

    @Column(length = 20)
    private String motherPhone;

    @Column(length = 64)
    private String motherOccupation;

    @Column(length = 255)
    private String motherPhoto;

    // --- Guardian (relation radio: Father / Mother / Others) ---
    @Column(length = 32)
    private String guardianRelation;

    @Column(length = 64)
    private String guardianName;

    @Column(length = 64)
    private String guardianEmail;

    @Column(length = 20)
    private String guardianPhone;

    @Column(length = 64)
    private String guardianOccupation;

    @Column(length = 255)
    private String guardianAddress;

    @Column(length = 255)
    private String guardianPhoto;

    // --- Medical Details ---
    @Column(length = 8)
    private String bloodGroup;

    @Column(length = 16)
    private String height;

    @Column(length = 16)
    private String weight;

    // --- Bank Details ---
    @Column(length = 32)
    private String bankAccountNumber;

    @Column(length = 64)
    private String bankName;

    @Column(length = 16)
    private String ifscCode;

    @Column(length = 32)
    private String nationalIdNumber;

    // --- Previous School Details ---
    @Column(length = 128)
    private String prevSchoolName;

    @Column(length = 255)
    private String prevSchoolAddress;

    // --- Address ---
    @Column(length = 255)
    private String currentAddress;

    @Column(length = 255)
    private String permanentAddress;

    // --- Hostel Details ---
    @Column(length = 64)
    private String hostelName;

    @Column(length = 16)
    private String roomNo;

    // --- Documents & Notes ---
    @Column(length = 128)
    private String docName;

    @Column(length = 255)
    private String docFile;

    @Column(length = 2000)
    private String studentNotes;

    @Column(name = "attendance_percentage")
    private Double attendancePercentage;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

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