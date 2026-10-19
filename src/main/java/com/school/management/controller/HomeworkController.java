package com.school.management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** Compatibility routes share assignment rules and persistence with mobile. */
@RestController
@RequestMapping("/api/teacher/homework")
public class HomeworkController {
    private final MobileAppController assignments;
    public HomeworkController(MobileAppController assignments) {
        this.assignments = assignments;
    }
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return assignments.getTeacherAssignments(null, null);
    }
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        return assignments.createTeacherAssignment(request);
    }
}
