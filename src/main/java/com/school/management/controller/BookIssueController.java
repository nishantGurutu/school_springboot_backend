package com.school.management.controller;

import com.school.management.dto.BookIssueRequest;
import com.school.management.dto.BookIssueResponse;
import com.school.management.service.BookIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library/issues")
@RequiredArgsConstructor
@Tag(name = "Book Issues", description = "Library book issue management APIs")
public class BookIssueController {

    private final BookIssueService bookIssueService;

    @GetMapping
    @Operation(summary = "List all book issues")
    public ResponseEntity<List<BookIssueResponse>> getAll() {
        return ResponseEntity.ok(bookIssueService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book issue by id")
    public ResponseEntity<BookIssueResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookIssueService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new book issue")
    public ResponseEntity<BookIssueResponse> create(@Valid @RequestBody BookIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookIssueService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a book issue")
    public ResponseEntity<BookIssueResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BookIssueRequest request) {
        return ResponseEntity.ok(bookIssueService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book issue")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookIssueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}