package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SchoolClassRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String section;
}
