package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeeCollectionRequest {

    @Size(max = 32, message = "Admission number must be at most 32 characters")
    private String admissionNo;

    @NotBlank(message = "Name is required")
    @Size(max = 64, message = "Name must be at most 64 characters")
    private String name;

    @Size(max = 16, message = "Roll number must be at most 16 characters")
    private String rollNo;

    @Size(max = 32, message = "Class name must be at most 32 characters")
    private String className;

    @Size(max = 16, message = "Amount must be at most 16 characters")
    private String amount;

    @Size(max = 16, message = "Paid must be at most 16 characters")
    private String paid;

    @Size(max = 16, message = "Due must be at most 16 characters")
    private String due;

    @Size(max = 32, message = "Date must be at most 32 characters")
    private String date;

    @Size(max = 16, message = "Status must be at most 16 characters")
    private String status;

    @Size(max = 32, message = "Payment type must be at most 32 characters")
    private String paymentType;

    @Size(max = 255, message = "Note must be at most 255 characters")
    private String note;

    @Size(max = 255, message = "Avatar URL must be at most 255 characters")
    private String avatar;
}