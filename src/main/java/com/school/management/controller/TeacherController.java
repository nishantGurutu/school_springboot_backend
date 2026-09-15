package com.school.management.controller;

import com.school.management.dto.TeacherRequest;
import com.school.management.dto.TeacherResponse;
import com.school.management.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers", description = "Teacher management APIs")
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @Operation(summary = "Create a new teacher")
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get teacher by id")
    public ResponseEntity<TeacherResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get teacher by employee ID")
    public ResponseEntity<TeacherResponse> getByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(teacherService.getByEmployeeId(employeeId));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get teacher by email")
    public ResponseEntity<TeacherResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(teacherService.getByEmail(email));
    }

    @GetMapping
    @Operation(summary = "List all teachers with pagination")
    public ResponseEntity<Page<TeacherResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(teacherService.getAll(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @Operation(summary = "Search teachers by name, employee ID, department, subject or email")
    public ResponseEntity<Page<TeacherResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(teacherService.search(q, page, size));
    }

    @GetMapping("/department/{department}")
    @Operation(summary = "Get teachers by department")
    public ResponseEntity<Page<TeacherResponse>> getByDepartment(
            @PathVariable String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(teacherService.getByDepartment(department, page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a teacher")
    public ResponseEntity<TeacherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(teacherService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update teacher status")
    public ResponseEntity<TeacherResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(teacherService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a teacher")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}