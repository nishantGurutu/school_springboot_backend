package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DesignationRequest {

    @NotBlank(message = "Designation name is required")
    @Size(max = 128, message = "Name must be at most 128 characters")
    private String name;

    @Size(max = 32, message = "Code must be at most 32 characters")
    private String code;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private String category;

    private String description;

    private String status;
}
