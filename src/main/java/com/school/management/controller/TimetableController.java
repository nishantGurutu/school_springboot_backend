package com.school.management.controller;

import com.school.management.dto.TimetableRequest;
import com.school.management.dto.TimetableResponse;
import com.school.management.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
@Tag(name = "Academic Timetable", description = "Class Timetable scheduling and management APIs")
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping
    @Operation(summary = "List timetable slots with optional filters")
    public ResponseEntity<List<TimetableResponse>> getAll(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String day,
            @RequestParam(required = false) String teacherName) {
        return ResponseEntity.ok(timetableService.getAll(className, section, day, teacherName));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get timetable slot by ID")
    public ResponseEntity<TimetableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(timetableService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new timetable slot")
    public ResponseEntity<TimetableResponse> create(@Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing timetable slot")
    public ResponseEntity<TimetableResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.ok(timetableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a timetable slot")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timetableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/class/{className}/section/{section}")
    @Operation(summary = "Get timetable slots by Class and Section")
    public ResponseEntity<List<TimetableResponse>> getByClassAndSection(
            @PathVariable String className,
            @PathVariable String section) {
        return ResponseEntity.ok(timetableService.getByClassAndSection(className, section));
    }

    @GetMapping("/teacher/{teacherName}")
    @Operation(summary = "Get timetable slots for a specific teacher")
    public ResponseEntity<List<TimetableResponse>> getByTeacher(
            @PathVariable String teacherName) {
        return ResponseEntity.ok(timetableService.getByTeacher(teacherName));
    }
}
