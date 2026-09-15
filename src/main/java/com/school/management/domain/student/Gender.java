package com.school.management.domain.student;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    @JsonCreator
    public static Gender fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.school.management.exceptions.BadRequestException(
                    "Invalid gender '" + value + "'. Allowed values: Male, Female, Other");
        }
    }
}