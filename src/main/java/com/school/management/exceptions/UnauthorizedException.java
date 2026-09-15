package com.school.management.exceptions;

import com.school.management.exceptions.ApiException;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
