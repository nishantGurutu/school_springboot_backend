package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaveRequestRequest {

    private String applyDate;

    @NotBlank(message = "Name is required")
    private String name;

    private String userType;

    private String leaveType;

    private String date;

    private String duration;

    private String status;

    private String reason;

    private String note;
}
