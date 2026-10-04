package com.school.management.controller;

import com.school.management.dto.DesignationRequest;
import com.school.management.dto.DesignationResponse;
import com.school.management.service.DesignationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/designations", "/api/hrm/designations", "/api/classes/designations"})
@RequiredArgsConstructor
@Tag(name = "Designations", description = "School staff and teacher designation management APIs")
public class DesignationController {

    private final DesignationService designationService;

    @GetMapping
    @Operation(summary = "List all designations")
    public ResponseEntity<List<DesignationResponse>> getAll() {
        return ResponseEntity.ok(designationService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get designation details by ID")
    public ResponseEntity<DesignationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new designation")
    public ResponseEntity<DesignationResponse> create(@Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designationService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a designation")
    public ResponseEntity<DesignationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(designationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a designation")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        designationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
