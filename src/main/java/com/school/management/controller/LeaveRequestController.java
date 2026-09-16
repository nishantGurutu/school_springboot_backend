package com.school.management.controller;

import com.school.management.dto.LeaveRequestRequest;
import com.school.management.dto.LeaveRequestResponse;
import com.school.management.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves/requests")
@RequiredArgsConstructor
@Tag(name = "Leave Requests", description = "Leave request management APIs")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @Operation(summary = "Create a new leave request")
    public ResponseEntity<LeaveRequestResponse> create(@Valid @RequestBody LeaveRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveRequestService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all leave requests")
    public ResponseEntity<List<LeaveRequestResponse>> getAll() {
        return ResponseEntity.ok(leaveRequestService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave request by id")
    public ResponseEntity<LeaveRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a leave request")
    public ResponseEntity<LeaveRequestResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequestRequest request) {
        return ResponseEntity.ok(leaveRequestService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a leave request")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leaveRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
