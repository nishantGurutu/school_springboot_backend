package com.school.management.controller;

import com.school.management.dto.SectionRequest;
import com.school.management.dto.SectionResponse;
import com.school.management.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes/sections")
@RequiredArgsConstructor
@Tag(name = "Sections", description = "Section management APIs")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping
    @Operation(summary = "List all sections")
    public ResponseEntity<List<SectionResponse>> getAll() {
        return ResponseEntity.ok(sectionService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new section")
    public ResponseEntity<SectionResponse> create(@Valid @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a section")
    public ResponseEntity<SectionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.ok(sectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a section")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
