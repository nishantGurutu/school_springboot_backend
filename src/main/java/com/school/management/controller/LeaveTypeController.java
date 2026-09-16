package com.school.management.controller;

import com.school.management.dto.LeaveTypeRequest;
import com.school.management.dto.LeaveTypeResponse;
import com.school.management.service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves/types")
@RequiredArgsConstructor
@Tag(name = "Leave Types", description = "Leave type management APIs")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @PostMapping
    @Operation(summary = "Create a new leave type")
    public ResponseEntity<LeaveTypeResponse> create(@Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveTypeService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all leave types")
    public ResponseEntity<List<LeaveTypeResponse>> getAll() {
        return ResponseEntity.ok(leaveTypeService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave type by id")
    public ResponseEntity<LeaveTypeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveTypeService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a leave type")
    public ResponseEntity<LeaveTypeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.ok(leaveTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a leave type")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leaveTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
