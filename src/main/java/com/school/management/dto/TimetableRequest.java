package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TimetableRequest {

    @NotBlank(message = "Class name is required (e.g. Class 6)")
    private String className;

    @NotBlank(message = "Section is required (e.g. A)")
    private String section;

    @NotBlank(message = "Day of week is required (e.g. Monday or Mon)")
    private String dayOfWeek;

    private String periodName;

    @NotBlank(message = "Subject is required")
    private String subject;

    private Long teacherId;

    private String teacherName;

    private String classroom;

    @NotBlank(message = "Start time is required (e.g. 09:00 AM)")
    private String startTime;

    @NotBlank(message = "End time is required (e.g. 09:45 AM)")
    private String endTime;

    private String status;
}
