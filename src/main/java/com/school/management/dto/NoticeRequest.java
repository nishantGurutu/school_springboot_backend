package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String date;
    private String target;
    private String category;
}