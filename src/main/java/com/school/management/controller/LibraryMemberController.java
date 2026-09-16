package com.school.management.controller;

import com.school.management.dto.LibraryMemberRequest;
import com.school.management.dto.LibraryMemberResponse;
import com.school.management.service.LibraryMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library/members")
@RequiredArgsConstructor
@Tag(name = "Library Members", description = "Library member management APIs")
public class LibraryMemberController {

    private final LibraryMemberService libraryMemberService;

    @GetMapping
    @Operation(summary = "List all library members")
    public ResponseEntity<List<LibraryMemberResponse>> getAll() {
        return ResponseEntity.ok(libraryMemberService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get library member by id")
    public ResponseEntity<LibraryMemberResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(libraryMemberService.getById(id));
    }

    @GetMapping("/card/{cardNo}")
    @Operation(summary = "Get library member by card number")
    public ResponseEntity<LibraryMemberResponse> getByCardNo(@PathVariable String cardNo) {
        return ResponseEntity.ok(libraryMemberService.getByCardNo(cardNo));
    }

    @PostMapping
    @Operation(summary = "Create a new library member")
    public ResponseEntity<LibraryMemberResponse> create(@Valid @RequestBody LibraryMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryMemberService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a library member")
    public ResponseEntity<LibraryMemberResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LibraryMemberRequest request) {
        return ResponseEntity.ok(libraryMemberService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a library member")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        libraryMemberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}