package com.school.management.dto;

import com.school.management.domain.student.Gender;
import com.school.management.domain.student.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentRequest {

    // --- Personal Info ---
    @Size(max = 32, message = "Academic year must be at most 32 characters")
    private String academicYear;

    @NotBlank(message = "Class is required")
    @Size(max = 32, message = "Class must be at most 32 characters")
    private String studentClass;

    @Size(max = 32, message = "Section must be at most 32 characters")
    private String section;

    @NotBlank(message = "Roll number is required")
    @Size(max = 20, message = "Roll number must be at most 20 characters")
    private String rollNumber;

    @NotBlank(message = "Admission number is required")
    @Size(max = 32, message = "Admission number must be at most 32 characters")
    private String admissionNo;

    @NotBlank(message = "Full name is required")
    @Size(max = 128, message = "Full name must be at most 128 characters")
    private String fullName;

    @Size(max = 32, message = "Category must be at most 32 characters")
    private String category;

    private Gender gender;

    @Size(max = 16, message = "Date of birth must be at most 16 characters")
    private String dob;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[^\\s]{3,25}$", message = "Invalid phone number")
    private String phone;

    @Email(message = "Invalid email address")
    @Size(max = 64, message = "Email must be at most 64 characters")
    private String email;

    @Size(max = 255, message = "Photo value too long")
    private String studentPhoto;

    // --- Parent & Guardian Info ---
    @Size(max = 64, message = "Father name must be at most 64 characters")
    private String fatherName;

    @Size(max = 20, message = "Father phone must be at most 20 characters")
    private String fatherPhone;

    @Size(max = 64, message = "Father occupation must be at most 64 characters")
    private String fatherOccupation;

    @Size(max = 255, message = "Father photo value too long")
    private String fatherPhoto;

    @Size(max = 64, message = "Mother name must be at most 64 characters")
    private String motherName;

    @Size(max = 20, message = "Mother phone must be at most 20 characters")
    private String motherPhone;

    @Size(max = 64, message = "Mother occupation must be at most 64 characters")
    private String motherOccupation;

    @Size(max = 255, message = "Mother photo value too long")
    private String motherPhoto;

    // --- Guardian ---
    @Size(max = 32, message = "Guardian relation must be at most 32 characters")
    private String guardianRelation;

    @Size(max = 64, message = "Guardian name must be at most 64 characters")
    private String guardianName;

    @Email(message = "Invalid guardian email address")
    @Size(max = 64, message = "Guardian email must be at most 64 characters")
    private String guardianEmail;

    @Size(max = 20, message = "Guardian phone must be at most 20 characters")
    private String guardianPhone;

    @Size(max = 64, message = "Guardian occupation must be at most 64 characters")
    private String guardianOccupation;

    @Size(max = 255, message = "Guardian address must be at most 255 characters")
    private String guardianAddress;

    @Size(max = 255, message = "Guardian photo value too long")
    private String guardianPhoto;

    // --- Medical Details ---
    @Size(max = 8, message = "Blood group must be at most 8 characters")
    private String bloodGroup;

    @Size(max = 16, message = "Height must be at most 16 characters")
    private String height;

    @Size(max = 16, message = "Weight must be at most 16 characters")
    private String weight;

    // --- Bank Details ---
    @Size(max = 32, message = "Bank account number must be at most 32 characters")
    private String bankAccountNumber;

    @Size(max = 64, message = "Bank name must be at most 64 characters")
    private String bankName;

    @Size(max = 16, message = "IFSC code must be at most 16 characters")
    private String ifscCode;

    @Size(max = 32, message = "National ID number must be at most 32 characters")
    private String nationalIdNumber;

    // --- Previous School Details ---
    @Size(max = 128, message = "Previous school name must be at most 128 characters")
    private String prevSchoolName;

    @Size(max = 255, message = "Previous school address must be at most 255 characters")
    private String prevSchoolAddress;

    // --- Address ---
    @Size(max = 255, message = "Current address must be at most 255 characters")
    private String currentAddress;

    @Size(max = 255, message = "Permanent address must be at most 255 characters")
    private String permanentAddress;

    // --- Hostel Details ---
    @Size(max = 64, message = "Hostel name must be at most 64 characters")
    private String hostelName;

    @Size(max = 16, message = "Room number must be at most 16 characters")
    private String roomNo;

    // --- Documents ---
    @Size(max = 128, message = "Document name must be at most 128 characters")
    private String docName;

    @Size(max = 255, message = "Document file value too long")
    private String docFile;

    // --- Notes ---
    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String studentNotes;

    private Double attendancePercentage;

    private StudentStatus status;
}