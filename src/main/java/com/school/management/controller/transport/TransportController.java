package com.school.management.controller.transport;

import com.school.management.dto.transport.*;
import com.school.management.service.transport.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    // Helper to resolve user email or fallback for dev/testing
    private String resolveEmail(UserDetails userDetails) {
        return userDetails != null ? userDetails.getUsername() : "driver@school.com";
    }

    // ==========================================
    // OVERVIEW
    // ==========================================

    @GetMapping("/overview")
    public ResponseEntity<TransportOverviewDto> getOverview(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getOverview(schoolId));
    }

    // ==========================================
    // BUSES
    // ==========================================

    @GetMapping("/buses")
    public ResponseEntity<List<BusDto>> getBuses(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getAllBuses(schoolId));
    }

    @GetMapping("/buses/{id}")
    public ResponseEntity<BusDto> getBus(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getBusById(id, schoolId));
    }

    @PostMapping("/buses")
    public ResponseEntity<BusDto> createBus(
            @RequestBody BusDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportService.createBus(dto, schoolId));
    }

    @PutMapping("/buses/{id}")
    public ResponseEntity<BusDto> updateBus(
            @PathVariable Long id,
            @RequestBody BusDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.updateBus(id, dto, schoolId));
    }

    @DeleteMapping("/buses/{id}")
    public ResponseEntity<Map<String, String>> deleteBus(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        transportService.deleteBus(id, schoolId);
        return ResponseEntity.ok(Map.of("message", "Bus deleted successfully"));
    }

    // ==========================================
    // DRIVERS
    // ==========================================

    @GetMapping("/drivers")
    public ResponseEntity<List<DriverDto>> getDrivers(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getAllDrivers(schoolId));
    }

    @GetMapping("/drivers/{id}")
    public ResponseEntity<DriverDto> getDriver(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getDriverById(id, schoolId));
    }

    @GetMapping("/drivers/{id}/daily-details")
    public ResponseEntity<DriverDailyDetailsDto> getDriverDailyDetails(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getDriverDailyDetails(id, schoolId));
    }

    @PostMapping("/drivers")
    public ResponseEntity<DriverDto> createDriver(
            @RequestBody DriverDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportService.createDriver(dto, schoolId));
    }

    @PutMapping("/drivers/{id}")
    public ResponseEntity<DriverDto> updateDriver(
            @PathVariable Long id,
            @RequestBody DriverDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.updateDriver(id, dto, schoolId));
    }

    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<Map<String, String>> deleteDriver(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        transportService.deleteDriver(id, schoolId);
        return ResponseEntity.ok(Map.of("message", "Driver deleted successfully"));
    }

    // ==========================================
    // ROUTES & STOPS
    // ==========================================

    @GetMapping("/routes")
    public ResponseEntity<List<RouteDto>> getRoutes(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getAllRoutes(schoolId));
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<RouteDto> getRoute(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getRouteById(id, schoolId));
    }

    @PostMapping("/routes")
    public ResponseEntity<RouteDto> createRoute(
            @RequestBody RouteDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportService.createRoute(dto, schoolId));
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<RouteDto> updateRoute(
            @PathVariable Long id,
            @RequestBody RouteDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.updateRoute(id, dto, schoolId));
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Map<String, String>> deleteRoute(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        transportService.deleteRoute(id, schoolId);
        return ResponseEntity.ok(Map.of("message", "Route deleted successfully"));
    }

    @PostMapping("/routes/{routeId}/stops")
    public ResponseEntity<List<RouteStopDto>> saveStops(
            @PathVariable Long routeId,
            @RequestBody List<RouteStopDto> stops,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.saveRouteStops(routeId, stops, schoolId));
    }

    // ==========================================
    // ASSIGNMENTS
    // ==========================================

    @GetMapping("/assignments")
    public ResponseEntity<List<BusAssignmentDto>> getAssignments(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getAllAssignments(schoolId));
    }

    @PostMapping("/assignments")
    public ResponseEntity<BusAssignmentDto> createAssignment(
            @RequestBody BusAssignmentDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportService.createAssignment(dto, schoolId));
    }

    @PutMapping("/assignments/{id}")
    public ResponseEntity<BusAssignmentDto> updateAssignment(
            @PathVariable Long id,
            @RequestBody BusAssignmentDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.updateAssignment(id, dto, schoolId));
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Map<String, String>> deleteAssignment(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        transportService.deleteAssignment(id, schoolId);
        return ResponseEntity.ok(Map.of("message", "Assignment removed successfully"));
    }

    // ==========================================
    // STUDENT ASSIGNMENTS
    // ==========================================

    @GetMapping("/student-assignments")
    public ResponseEntity<List<StudentAssignmentDto>> getStudentAssignments(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getStudentAssignments(schoolId));
    }

    @PostMapping("/student-assignments")
    public ResponseEntity<StudentAssignmentDto> assignStudent(
            @RequestBody StudentAssignmentDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportService.assignStudentToRoute(dto, schoolId));
    }

    @PutMapping("/student-assignments/{id}")
    public ResponseEntity<StudentAssignmentDto> updateStudentAssignment(
            @PathVariable Long id,
            @RequestBody StudentAssignmentDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.updateStudentAssignment(id, dto, schoolId));
    }

    @DeleteMapping("/student-assignments/{id}")
    public ResponseEntity<Map<String, String>> removeStudentAssignment(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        transportService.removeStudentAssignment(id, schoolId);
        return ResponseEntity.ok(Map.of("message", "Student transport assignment removed"));
    }

    @PatchMapping("/student-assignments/{id}/status")
    public ResponseEntity<Map<String, String>> updateStudentStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        transportService.updateStudentPickupStatus(id, status, schoolId);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
    }

    // ==========================================
    // TRANSPORT FEES & PAYMENTS
    // ==========================================

    @GetMapping("/fees/summary")
    public ResponseEntity<TransportFeeSummaryDto> getFeeSummary(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getFeeSummary(schoolId));
    }

    @PostMapping("/fees/collect")
    public ResponseEntity<TransportPaymentDto> collectFee(
            @RequestBody TransportPaymentDto dto,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportService.collectFee(dto, schoolId));
    }

    // ==========================================
    // DRIVER OPERATIONS
    // ==========================================

    @GetMapping("/driver/my-duty")
    public ResponseEntity<TripDtos.DriverDutyDto> getMyDuty(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email) {
        String driverEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        return ResponseEntity.ok(transportService.getDriverDuty(driverEmail));
    }

    @PostMapping("/trips/start")
    public ResponseEntity<TripDtos.TripSummaryDto> startTrip(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email,
            @RequestBody TripDtos.StartTripRequest request) {
        String driverEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        return ResponseEntity.ok(transportService.startTrip(driverEmail, request));
    }

    @PostMapping("/trips/{tripId}/locations:batch")
    public ResponseEntity<Map<String, Object>> ingestGpsBatch(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email,
            @RequestBody TripDtos.BatchGpsRequest request) {
        String driverEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        int saved = transportService.ingestGpsBatch(tripId, driverEmail, request);
        return ResponseEntity.ok(Map.of("success", true, "pointsProcessed", saved));
    }

    @PostMapping("/trips/{tripId}/stops/{stopId}/events")
    public ResponseEntity<TripDtos.StopEventDto> recordStopEvent(
            @PathVariable Long tripId,
            @PathVariable Long stopId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email,
            @RequestBody TripDtos.StopEventRequest request) {
        String driverEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        return ResponseEntity.ok(transportService.recordStopEvent(tripId, stopId, driverEmail, request));
    }

    @PostMapping("/trips/{tripId}/incidents")
    public ResponseEntity<TripDtos.TripIncidentDto> reportIncident(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email,
            @RequestBody TripDtos.TripIncidentRequest request) {
        String driverEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        return ResponseEntity.ok(transportService.reportIncident(tripId, driverEmail, request));
    }

    @PostMapping("/trips/{tripId}/end")
    public ResponseEntity<TripDtos.TripSummaryDto> endTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email,
            @RequestBody TripDtos.EndTripRequest request) {
        String driverEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        return ResponseEntity.ok(transportService.endTrip(tripId, driverEmail, request));
    }

    // ==========================================
    // TRACKING & PLAYBACK
    // ==========================================

    @GetMapping("/tracking/live")
    public ResponseEntity<List<TripDtos.LiveTrackingBusDto>> getLiveTracking(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getLiveTracking(schoolId));
    }

    @GetMapping("/tracking/trips/{tripId}/playback")
    public ResponseEntity<TripDtos.TripPlaybackDto> getTripPlayback(
            @PathVariable Long tripId,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        return ResponseEntity.ok(transportService.getTripPlayback(tripId, schoolId));
    }

    @GetMapping("/reports/trips")
    public ResponseEntity<List<TripDtos.TripSummaryDto>> getTripHistory(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long busId,
            @RequestParam(required = false) Long driverId) {
        return ResponseEntity.ok(transportService.getTripHistory(schoolId, date, busId, driverId));
    }

    // ==========================================
    // ALERTS
    // ==========================================

    @GetMapping("/alerts")
    public ResponseEntity<List<TransportAlertDto>> getAlerts(
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(transportService.getAlerts(schoolId, status));
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<TransportAlertDto> acknowledgeAlert(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) AlertActionRequest request,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        String user = resolveEmail(userDetails);
        String remarks = request != null ? request.getRemarks() : "Acknowledged by staff";
        return ResponseEntity.ok(transportService.acknowledgeAlert(alertId, user, remarks, schoolId));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<TransportAlertDto> resolveAlert(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) AlertActionRequest request,
            @RequestParam(required = false, defaultValue = "SCH-001") String schoolId) {
        String user = resolveEmail(userDetails);
        String remarks = request != null ? request.getRemarks() : "Resolved";
        return ResponseEntity.ok(transportService.resolveAlert(alertId, user, remarks, schoolId));
    }

    // ==========================================
    // PARENT & STUDENT ENDPOINTS
    // ==========================================

    @GetMapping("/parent/children/{studentId}/transport")
    public ResponseEntity<ParentTransportDto> getParentChildTransport(
            @PathVariable Long studentId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email) {
        String pEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        return ResponseEntity.ok(transportService.getParentChildTransport(pEmail, studentId));
    }

    @GetMapping("/student/my-transport")
    public ResponseEntity<StudentTransportDto> getStudentTransport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email) {
        String sEmail = (email != null && !email.isBlank()) ? email : resolveEmail(userDetails);
        return ResponseEntity.ok(transportService.getStudentTransport(sEmail));
    }
}
