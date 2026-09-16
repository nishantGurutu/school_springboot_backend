package com.school.management.controller;

import com.school.management.dto.ExamResultRequest;
import com.school.management.dto.ExamResultResponse;
import com.school.management.service.ExamResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams/results")
@RequiredArgsConstructor
@Tag(name = "Exam Results", description = "Exam result management APIs")
public class ExamResultController {

    private final ExamResultService examResultService;

    @GetMapping
    @Operation(summary = "List all exam results")
    public ResponseEntity<List<ExamResultResponse>> getAll() {
        return ResponseEntity.ok(examResultService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new exam result")
    public ResponseEntity<ExamResultResponse> create(@Valid @RequestBody ExamResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examResultService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exam result")
    public ResponseEntity<ExamResultResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ExamResultRequest request) {
        return ResponseEntity.ok(examResultService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exam result")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examResultService.delete(id);
        return ResponseEntity.noContent().build();
    }
}