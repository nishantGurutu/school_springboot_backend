package com.school.management.dto;

import com.school.management.domain.teacher.JobType;
import com.school.management.domain.teacher.TeacherStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeacherRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 32, message = "Employee ID must be at most 32 characters")
    private String employeeId;

    @NotBlank(message = "First name is required")
    @Size(max = 64, message = "First name must be at most 64 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 64, message = "Last name must be at most 64 characters")
    private String lastName;

    @NotBlank(message = "Department is required")
    @Size(max = 64, message = "Department must be at most 64 characters")
    private String department;

    @NotBlank(message = "Subject is required")
    @Size(max = 64, message = "Subject must be at most 64 characters")
    private String subject;

    @Size(max = 128, message = "Qualification must be at most 128 characters")
    private String qualification;

    @Size(max = 64, message = "Designation must be at most 64 characters")
    private String designation;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 64, message = "Email must be at most 64 characters")
    private String email;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private Integer experienceYears;

    @Size(max = 64, message = "Blood group must be at most 64 characters")
    private String bloodGroup;

    private JobType jobType;

    @Size(max = 255, message = "Avatar URL must be at most 255 characters")
    private String avatar;

    private TeacherStatus status;
}