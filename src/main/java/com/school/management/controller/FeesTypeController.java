package com.school.management.controller;

import com.school.management.dto.FeesTypeRequest;
import com.school.management.dto.FeesTypeResponse;
import com.school.management.service.FeesTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/types")
@RequiredArgsConstructor
@Tag(name = "Fees Types", description = "Fees type management APIs")
public class FeesTypeController {

    private final FeesTypeService feesTypeService;

    @GetMapping
    @Operation(summary = "List all fees types")
    public ResponseEntity<List<FeesTypeResponse>> getAll() {
        return ResponseEntity.ok(feesTypeService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new fees type")
    public ResponseEntity<FeesTypeResponse> create(@Valid @RequestBody FeesTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feesTypeService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fees type")
    public ResponseEntity<FeesTypeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FeesTypeRequest request) {
        return ResponseEntity.ok(feesTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fees type")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feesTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}