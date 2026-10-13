package com.school.management.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Health Check", description = "Server Health Check Endpoint for Cron Jobs & Uptime Monitoring")
public class HealthCheckController {

    // URL: /api/health
    @GetMapping("/api/health")
    @Operation(summary = "Check server health status", description = "Returns active status to keep free-tier instances alive")
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Server is active");
        return ResponseEntity.ok(response);
    }
}
