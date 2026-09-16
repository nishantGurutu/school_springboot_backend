package com.school.management.controller;

import com.school.management.dto.FeesGroupRequest;
import com.school.management.dto.FeesGroupResponse;
import com.school.management.service.FeesGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/groups")
@RequiredArgsConstructor
@Tag(name = "Fees Groups", description = "Fees group management APIs")
public class FeesGroupController {

    private final FeesGroupService feesGroupService;

    @GetMapping
    @Operation(summary = "List all fees groups")
    public ResponseEntity<List<FeesGroupResponse>> getAll() {
        return ResponseEntity.ok(feesGroupService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new fees group")
    public ResponseEntity<FeesGroupResponse> create(@Valid @RequestBody FeesGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feesGroupService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fees group")
    public ResponseEntity<FeesGroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FeesGroupRequest request) {
        return ResponseEntity.ok(feesGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fees group")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feesGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}