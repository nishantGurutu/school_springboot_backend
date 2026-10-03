package com.school.management.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;
    private String path;
    private List<String> errors;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiResponse(boolean success, String message, T data) {
        this(success, message, data, 200, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Operation successful", data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(201)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(400)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> fail(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> fail(String message, int statusCode, List<String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}