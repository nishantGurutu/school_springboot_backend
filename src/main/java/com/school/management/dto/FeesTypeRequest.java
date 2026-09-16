package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeesTypeRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 64, message = "Name must be at most 64 characters")
    private String name;

    @Size(max = 16, message = "Status must be at most 16 characters")
    private String status;
}