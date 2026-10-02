package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExamScheduleRequest {

    private String examName;

    private String className;

    private String section;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String date;

    private String startTime;

    private String endTime;

    private String duration;

    private String room;
}