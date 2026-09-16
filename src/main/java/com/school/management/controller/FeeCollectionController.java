package com.school.management.controller;

import com.school.management.dto.FeeCollectionRequest;
import com.school.management.dto.FeeCollectionResponse;
import com.school.management.service.FeeCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/collections")
@RequiredArgsConstructor
@Tag(name = "Fee Collections", description = "Fee collection management APIs")
public class FeeCollectionController {

    private final FeeCollectionService feeCollectionService;

    @GetMapping
    @Operation(summary = "List all fee collections")
    public ResponseEntity<List<FeeCollectionResponse>> getAll() {
        return ResponseEntity.ok(feeCollectionService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new fee collection")
    public ResponseEntity<FeeCollectionResponse> create(@Valid @RequestBody FeeCollectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeCollectionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fee collection")
    public ResponseEntity<FeeCollectionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FeeCollectionRequest request) {
        return ResponseEntity.ok(feeCollectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fee collection")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feeCollectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}