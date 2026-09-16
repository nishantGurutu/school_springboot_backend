package com.school.management.controller;

import com.school.management.dto.ClassRoomRequest;
import com.school.management.dto.ClassRoomResponse;
import com.school.management.service.ClassRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes/rooms")
@RequiredArgsConstructor
@Tag(name = "Class Rooms", description = "Class room management APIs")
public class ClassRoomController {

    private final ClassRoomService classRoomService;

    @GetMapping
    @Operation(summary = "List all rooms")
    public ResponseEntity<List<ClassRoomResponse>> getAll() {
        return ResponseEntity.ok(classRoomService.getAll());
    }

    @PostMapping
    @Operation(summary = "Create a new room")
    public ResponseEntity<ClassRoomResponse> create(@Valid @RequestBody ClassRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classRoomService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a room")
    public ResponseEntity<ClassRoomResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClassRoomRequest request) {
        return ResponseEntity.ok(classRoomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a room")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classRoomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
