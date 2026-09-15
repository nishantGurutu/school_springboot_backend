package com.school.management.controller;

import com.school.management.dto.StudentRequest;
import com.school.management.dto.StudentResponse;
import com.school.management.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student management APIs")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @Operation(summary = "Create a new student")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student by id")
    public ResponseEntity<StudentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @GetMapping("/admission/{admissionNo}")
    @Operation(summary = "Get student by admission number")
    public ResponseEntity<StudentResponse> getByAdmissionNo(@PathVariable String admissionNo) {
        return ResponseEntity.ok(studentService.getByAdmissionNo(admissionNo));
    }

    @GetMapping
    @Operation(summary = "List all students with pagination")
    public ResponseEntity<Page<StudentResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(studentService.getAll(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @Operation(summary = "Search students by name, admission number, roll no, email or class")
    public ResponseEntity<Page<StudentResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(studentService.search(q, page, size));
    }

    @GetMapping("/class/{className}")
    @Operation(summary = "Get students by class name")
    public ResponseEntity<Page<StudentResponse>> getByClass(
            @PathVariable String className,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(studentService.getByClass(className, page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a student")
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update student status")
    public ResponseEntity<StudentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(studentService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a student")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}