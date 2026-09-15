package com.school.management.domain.guardian;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GuardianType {
    FATHER,
    MOTHER,
    BROTHER,
    SISTER,
    OTHER;

    @JsonCreator
    public static GuardianType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GuardianType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.school.management.exceptions.BadRequestException(
                    "Invalid guardian type '" + value + "'. Allowed values: Father, Mother, Brother, Sister, Other");
        }
    }
}