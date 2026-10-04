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
import java.util.List;

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

    private Long departmentId;

    @Size(max = 500, message = "Subject must be at most 500 characters")
    private String subject;

    // Multi-select Subject IDs
    private List<Long> subjectIds;

    // Multi-select Class IDs
    private List<Long> assignedClassIds;
    private List<Long> classIds; // alias

    @Size(max = 500, message = "Assigned class must be at most 500 characters")
    private String assignedClass;

    @Size(max = 128, message = "Qualification must be at most 128 characters")
    private String qualification;

    @Size(max = 64, message = "Designation must be at most 64 characters")
    private String designation;

    private Long designationId;

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
    private String password;
    private String type; // Teacher, Principal, Staff

    // Form fields
    private String gender;
    private LocalDate dob;
    private String fatherName;
    private String motherName;
    private String maritalStatus;
    private String contractType;
    private String shift;
    private String workLocation;
    private String height;
    private String weight;
    private String bankAccountNumber;
    private String bankName;
    private String ifscCode;
    private String nationalIdNumber;
    private String docName;
    private String prevSchoolName;
    private String prevSchoolAddress;
    private String permanentAddress;
    private String teacherBio;
}