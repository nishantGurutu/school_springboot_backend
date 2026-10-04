package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DesignationRequest {

    @NotBlank(message = "Designation name is required")
    @Size(max = 128, message = "Name must be at most 128 characters")
    private String name;

    @Size(max = 32, message = "Code must be at most 32 characters")
    private String code;

    @Size(max = 64, message = "Category must be at most 64 characters")
    private String category;

    private String description;

    private String status;
}
