package com.school.management.entity;

import com.school.management.domain.teacher.JobType;
import com.school.management.domain.teacher.TeacherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Column(name = "department_id")
    private Long departmentId;

    @Column(nullable = false, length = 500)
    private String subject;

    // Multi-select Subject Specializations
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private Set<SubjectEntity> subjectSpecializations = new HashSet<>();

    @Column(name = "subject_ids", length = 500)
    private String subjectIds;

    // Multi-select Assigned Classes
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_classes",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    @Builder.Default
    private Set<SchoolClassEntity> assignedClasses = new HashSet<>();

    @Column(name = "assigned_class_ids", length = 500)
    private String assignedClassIds;

    @Column(name = "assigned_class", length = 500)
    private String assignedClass;

    @Column(length = 128)
    private String qualification;

    @Column(length = 64)
    private String designation;

    @Column(name = "designation_id")
    private Long designationId;

    @Column(length = 32)
    @Builder.Default
    private String type = "Teacher";

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

    // Form fields
    @Column(length = 16)
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "father_name", length = 128)
    private String fatherName;

    @Column(name = "mother_name", length = 128)
    private String motherName;

    @Column(name = "marital_status", length = 32)
    private String maritalStatus;

    @Column(name = "contract_type", length = 32)
    private String contractType;

    @Column(length = 32)
    private String shift;

    @Column(name = "work_location", length = 128)
    private String workLocation;

    @Column(length = 32)
    private String height;

    @Column(length = 32)
    private String weight;

    @Column(name = "bank_account_number", length = 64)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 128)
    private String bankName;

    @Column(name = "ifsc_code", length = 32)
    private String ifscCode;

    @Column(name = "national_id_number", length = 64)
    private String nationalIdNumber;

    @Column(name = "doc_name", length = 128)
    private String docName;

    @Column(name = "prev_school_name", length = 128)
    private String prevSchoolName;

    @Column(name = "prev_school_address", length = 255)
    private String prevSchoolAddress;

    @Column(name = "permanent_address", length = 255)
    private String permanentAddress;

    @Column(name = "teacher_bio", columnDefinition = "TEXT")
    private String teacherBio;

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