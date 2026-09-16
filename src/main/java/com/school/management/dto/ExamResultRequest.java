package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExamResultRequest {

    @NotBlank(message = "Admission number is required")
    private String admissionNo;

    @NotBlank(message = "Name is required")
    private String name;

    private String rollNo;

    private String className;

    private String exam;

    private Integer total;

    private Integer percent;

    private String grade;

    private String result;

    private String avatar;
}