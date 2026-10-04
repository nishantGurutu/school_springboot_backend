package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(max = 128, message = "Name must be at most 128 characters")
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(max = 32, message = "Code must be at most 32 characters")
    private String code;

    @Size(max = 128, message = "Head of department must be at most 128 characters")
    private String headOfDepartment;

    private String description;

    private String status;
}
