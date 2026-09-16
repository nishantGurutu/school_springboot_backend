package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LibraryMemberRequest {

    @NotBlank(message = "Card number is required")
    private String cardNo;

    @NotBlank(message = "Student name is required")
    private String studentName;

    private String joinDate;
    private String className;
    private String section;
    private String phone;
    private String email;
    private String gender;
    private String bookIssue;
    private String issueDate;
    private String returnDate;
    private String avatar;
}