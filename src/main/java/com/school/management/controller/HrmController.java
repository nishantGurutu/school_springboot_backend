package com.school.management.controller;

import com.school.management.response.ApiResponse;
import com.school.management.service.HrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/hrm")
@RequiredArgsConstructor
@Tag(name = "HRM", description = "Human resource management APIs")
public class HrmController {

    private final HrmService hrmService;

    @GetMapping("/summary")
    @Operation(summary = "Get HRM summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok("summary", hrmService.getSummary()));
    }
}