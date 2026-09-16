package com.school.management.controller;

import com.school.management.dto.ExamScheduleRequest;
import com.school.management.dto.ExamScheduleResponse;
import com.school.management.service.ExamScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams/schedules")
@RequiredArgsConstructor
@Tag(name = "Exam Schedules", description = "Exam schedule management APIs")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    @GetMapping
    @Operation(summary = "List all exam schedules")
    public ResponseEntity<List<ExamScheduleResponse>> getAll() {
        return ResponseEntity.ok(examScheduleService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new exam schedule")
    public ResponseEntity<ExamScheduleResponse> create(@Valid @RequestBody ExamScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examScheduleService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exam schedule")
    public ResponseEntity<ExamScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ExamScheduleRequest request) {
        return ResponseEntity.ok(examScheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exam schedule")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}