package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login credentials request body")
public class LoginRequest {

    @NotBlank(message = "Email or login ID is required")
    @Schema(description = "Email, Student Admission Number, Teacher Employee ID, or Login ID", example = "student@schooldesk.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "password")
    private String password;
}

