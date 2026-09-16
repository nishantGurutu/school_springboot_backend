package com.school.management.dto;

import com.school.management.entity.AttendanceEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceResponse {

    private Long id;
    private String attendanceType;
    private String admissionNo;
    private String name;
    private String rollNo;
    private String className;
    private String department;
    private String designation;
    private String attendanceDate;
    private String status;
    private String note;
    private String avatar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AttendanceResponse fromEntity(AttendanceEntity entity) {
        return AttendanceResponse.builder()
                .id(entity.getId())
                .attendanceType(entity.getAttendanceType())
                .admissionNo(entity.getAdmissionNo())
                .name(entity.getName())
                .rollNo(entity.getRollNo())
                .className(entity.getClassName())
                .department(entity.getDepartment())
                .designation(entity.getDesignation())
                .attendanceDate(entity.getAttendanceDate())
                .status(entity.getStatus())
                .note(entity.getNote())
                .avatar(entity.getAvatar())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}