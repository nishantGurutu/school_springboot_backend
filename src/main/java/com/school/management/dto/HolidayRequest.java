package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayRequest {

    @NotBlank(message = "Holiday title is required")
    private String title;

    @NotBlank(message = "Date is required (YYYY-MM-DD)")
    private String date;

    private String endDate;
    private String category;
    private String description;
    private String target;
}
