package com.school.management.exceptions;

import com.school.management.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message, List<String> errors) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(message)
                .statusCode(status.value())
                .errors(errors)
                .build();
        return new ResponseEntity<>(response, status);
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message) {
        return build(status, message, null);
    }

    @ExceptionHandler({InvalidEmailException.class, WrongPasswordException.class})
    public ResponseEntity<ApiResponse<Object>> handleLoginFailure(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserExists(UserAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.toList());
        String summary = String.join(", ", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, summary.isEmpty() ? "Validation failed" : summary, fieldErrors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("already")) {
            return build(HttpStatus.CONFLICT, ex.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Bad request");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred");
    }
}
