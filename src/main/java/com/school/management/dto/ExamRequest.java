package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExamRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String date;

    private String startTime;

    private String endTime;

    private String status;
}