package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookRequest {

    @NotBlank(message = "Book name is required")
    private String name;

    private String subject;
    private String publisher;
    private String author;
    private String bookNumber;
    private String rackNo;
    private Integer qty;
    private Integer available;
    private String price;
    private String date;
}