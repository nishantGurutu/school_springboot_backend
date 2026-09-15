package com.school.management.controller;

import com.school.management.dto.GuardianRequest;
import com.school.management.dto.GuardianResponse;
import com.school.management.service.GuardianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
@Tag(name = "Guardians / Parents", description = "Parent & Guardian management APIs")
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping
    @Operation(summary = "Create a new guardian/parent (optionally creates a PARENT login account)")
    public ResponseEntity<GuardianResponse> create(@Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardianService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get guardian by id")
    public ResponseEntity<GuardianResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(guardianService.getById(id));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get guardian by email")
    public ResponseEntity<GuardianResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(guardianService.getByEmail(email));
    }

    @GetMapping("/student/{admissionNo}")
    @Operation(summary = "Get all guardians of a student by admission number")
    public ResponseEntity<List<GuardianResponse>> getByStudent(@PathVariable String admissionNo) {
        return ResponseEntity.ok(guardianService.getByStudent(admissionNo));
    }

    @GetMapping
    @Operation(summary = "List all guardians with pagination")
    public ResponseEntity<Page<GuardianResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(guardianService.getAll(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @Operation(summary = "Search guardians by name, phone, email or occupation")
    public ResponseEntity<Page<GuardianResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(guardianService.search(q, page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a guardian")
    public ResponseEntity<GuardianResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.ok(guardianService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a guardian")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        guardianService.delete(id);
        return ResponseEntity.noContent().build();
    }
}