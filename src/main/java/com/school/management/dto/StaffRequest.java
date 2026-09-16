package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String staffType;
    private String designation;
    private String phone;
    private String email;
    private Double salary;
    private String joinDate;
    private String status;
}