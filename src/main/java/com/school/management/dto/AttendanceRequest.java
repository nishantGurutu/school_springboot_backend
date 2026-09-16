package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttendanceRequest {

    @NotBlank(message = "Attendance type is required")
    @Size(max = 16, message = "Attendance type must be at most 16 characters")
    private String attendanceType;

    @Size(max = 32, message = "Admission number must be at most 32 characters")
    private String admissionNo;

    @NotBlank(message = "Name is required")
    @Size(max = 64, message = "Name must be at most 64 characters")
    private String name;

    @Size(max = 16, message = "Roll number must be at most 16 characters")
    private String rollNo;

    @Size(max = 32, message = "Class name must be at most 32 characters")
    private String className;

    @Size(max = 64, message = "Department must be at most 64 characters")
    private String department;

    @Size(max = 64, message = "Designation must be at most 64 characters")
    private String designation;

    @Size(max = 32, message = "Attendance date must be at most 32 characters")
    private String attendanceDate;

    @Size(max = 16, message = "Status must be at most 16 characters")
    private String status;

    @Size(max = 255, message = "Note must be at most 255 characters")
    private String note;

    @Size(max = 255, message = "Avatar URL must be at most 255 characters")
    private String avatar;
}