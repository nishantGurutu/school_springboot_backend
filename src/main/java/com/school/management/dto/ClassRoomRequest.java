package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassRoomRequest {

    @NotBlank(message = "Room is required")
    private String room;

    private String capacity;
}
