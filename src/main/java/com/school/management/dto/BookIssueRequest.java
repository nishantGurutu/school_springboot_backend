package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookIssueRequest {

    private String cardNo;

    @NotBlank(message = "Issue to is required")
    private String issueTo;

    private String className;

    @NotBlank(message = "Book name is required")
    private String bookName;

    private String number;
    private String issueDate;
    private String returnDate;
    private String status;
}