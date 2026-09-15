package com.school.management.domain.teacher;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TeacherStatus {
    ACTIVE,
    ON_LEAVE,
    RESIGNED,
    RETIRED;

    @JsonCreator
    public static TeacherStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TeacherStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.school.management.exceptions.BadRequestException(
                    "Invalid status '" + value + "'. Allowed values: Active, On-Leave, Resigned, Retired");
        }
    }
}