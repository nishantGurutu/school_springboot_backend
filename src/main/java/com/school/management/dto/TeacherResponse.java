package com.school.management.dto;

import com.school.management.domain.teacher.JobType;
import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.entity.TeacherEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TeacherResponse {

    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String department;
    private String subject;
    private String qualification;
    private String designation;
    private String phone;
    private String email;
    private String address;
    private LocalDate joiningDate;
    private Integer experienceYears;
    private String bloodGroup;
    private JobType jobType;
    private String avatar;
    private TeacherStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TeacherResponse fromEntity(TeacherEntity teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .fullName(teacher.getFirstName() + " " + teacher.getLastName())
                .department(teacher.getDepartment())
                .subject(teacher.getSubject())
                .qualification(teacher.getQualification())
                .designation(teacher.getDesignation())
                .phone(teacher.getPhone())
                .email(teacher.getEmail())
                .address(teacher.getAddress())
                .joiningDate(teacher.getJoiningDate())
                .experienceYears(teacher.getExperienceYears())
                .bloodGroup(teacher.getBloodGroup())
                .jobType(teacher.getJobType())
                .avatar(teacher.getAvatar())
                .status(teacher.getStatus())
                .createdAt(teacher.getCreatedAt())
                .updatedAt(teacher.getUpdatedAt())
                .build();
    }
}