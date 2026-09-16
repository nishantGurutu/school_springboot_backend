package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExamScheduleRequest {

    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String date;

    private String startTime;

    private String endTime;

    private String duration;

    private String room;
}