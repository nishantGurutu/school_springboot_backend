package com.school.management.dto;

import com.school.management.domain.guardian.GuardianType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GuardianRequest {

    // Matches dashboard GuardianForm "Guardian Type" select
    private GuardianType guardianType;

    @NotBlank(message = "Guardian name is required")
    @Size(max = 64, message = "Guardian name must be at most 64 characters")
    private String guardianName;

    // Dashboard form labels this field "Instagram" but stores the phone number
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phone;

    @Size(max = 64, message = "Occupation must be at most 64 characters")
    private String occupation;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 64, message = "Email must be at most 64 characters")
    private String email;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 255, message = "Photo value too long")
    private String photo;

    @Size(max = 20, message = "Fee status must be at most 20 characters")
    private String feeStatus;

    // Link to the ward's student admission number (optional)
    @Size(max = 32, message = "Student admission number must be at most 32 characters")
    private String studentAdmissionNo;

    // Optional login credentials for the parent (creates a PARENT user)
    @Size(min = 4, max = 64, message = "Password must be between 4 and 64 characters")
    private String password;
}