package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeesDiscountRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 64, message = "Name must be at most 64 characters")
    private String name;

    @Size(max = 64, message = "Fees type must be at most 64 characters")
    private String feesType;

    @Size(max = 32, message = "Discount type must be at most 32 characters")
    private String discountType;

    @Size(max = 16, message = "Discount value must be at most 16 characters")
    private String discountValue;

    @Size(max = 16, message = "Status must be at most 16 characters")
    private String status;
}