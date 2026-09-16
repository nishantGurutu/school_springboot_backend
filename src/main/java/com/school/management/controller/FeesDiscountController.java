package com.school.management.controller;

import com.school.management.dto.FeesDiscountRequest;
import com.school.management.dto.FeesDiscountResponse;
import com.school.management.service.FeesDiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/discounts")
@RequiredArgsConstructor
@Tag(name = "Fees Discounts", description = "Fees discount management APIs")
public class FeesDiscountController {

    private final FeesDiscountService feesDiscountService;

    @GetMapping
    @Operation(summary = "List all fees discounts")
    public ResponseEntity<List<FeesDiscountResponse>> getAll() {
        return ResponseEntity.ok(feesDiscountService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new fees discount")
    public ResponseEntity<FeesDiscountResponse> create(@Valid @RequestBody FeesDiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feesDiscountService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fees discount")
    public ResponseEntity<FeesDiscountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FeesDiscountRequest request) {
        return ResponseEntity.ok(feesDiscountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fees discount")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feesDiscountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}