package com.school.management.domain.student;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StudentStatus {
    ACTIVE,
    INACTIVE,
    TRANSFERRED,
    WITHDRAWN,
    GRADUATED;

    @JsonCreator
    public static StudentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StudentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.school.management.exceptions.BadRequestException(
                    "Invalid status '" + value + "'. Allowed values: Active, Inactive, Transferred, Withdrawn, Graduated");
        }
    }
}