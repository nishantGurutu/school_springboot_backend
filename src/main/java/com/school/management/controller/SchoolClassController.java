package com.school.management.controller;

import com.school.management.dto.SchoolClassRequest;
import com.school.management.dto.SchoolClassResponse;
import com.school.management.service.SchoolClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes/classes")
@RequiredArgsConstructor
@Tag(name = "School Classes", description = "School class management APIs")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @GetMapping
    @Operation(summary = "List all classes")
    public ResponseEntity<List<SchoolClassResponse>> getAll() {
        return ResponseEntity.ok(schoolClassService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new class")
    public ResponseEntity<SchoolClassResponse> create(@Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolClassService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a class")
    public ResponseEntity<SchoolClassResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.ok(schoolClassService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a class")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        schoolClassService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
