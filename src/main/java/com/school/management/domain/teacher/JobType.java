package com.school.management.domain.teacher;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum JobType {
    FULL_TIME,
    PART_TIME,
    CONTRACT,
    INTERN;

    @JsonCreator
    public static JobType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return JobType.valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new com.school.management.exceptions.BadRequestException(
                    "Invalid job type '" + value + "'. Allowed values: Full-Time, Part-Time, Contract, Intern");
        }
    }
}