package com.school.management.controller;

import com.school.management.dto.HolidayRequest;
import com.school.management.dto.HolidayResponse;
import com.school.management.entity.HolidayEntity;
import com.school.management.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@Tag(name = "Holidays", description = "School Holiday management APIs")
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @Operation(summary = "List all school holidays")
    public ResponseEntity<List<HolidayResponse>> getAll() {
        return ResponseEntity.ok(holidayService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get school holiday by id")
    public ResponseEntity<HolidayResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(holidayService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new school holiday")
    public ResponseEntity<HolidayResponse> create(@Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a school holiday")
    public ResponseEntity<HolidayResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(holidayService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a school holiday")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        holidayService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check")
    @Operation(summary = "Check if a date is a school holiday")
    public ResponseEntity<Map<String, Object>> checkHoliday(@RequestParam String date) {
        Optional<HolidayEntity> holidayOpt = holidayService.getHolidayForDate(date);
        Map<String, Object> res = new HashMap<>();
        res.put("date", date);
        res.put("isHoliday", holidayOpt.isPresent());
        holidayOpt.ifPresent(holiday -> {
            res.put("holiday", HolidayResponse.fromEntity(holiday));
            res.put("title", holiday.getTitle());
            res.put("category", holiday.getCategory());
        });
        return ResponseEntity.ok(res);
    }
}
