package com.school.management.service.transport;

import com.school.management.dto.transport.*;

import java.time.LocalDate;
import java.util.List;

public interface TransportService {

    // Overview & Dashboard
    TransportOverviewDto getOverview(String schoolId);

    // Bus Management
    List<BusDto> getAllBuses(String schoolId);
    BusDto getBusById(Long id, String schoolId);
    BusDto createBus(BusDto dto, String schoolId);
    BusDto updateBus(Long id, BusDto dto, String schoolId);
    void deleteBus(Long id, String schoolId);

    // Driver Management
    List<DriverDto> getAllDrivers(String schoolId);
    DriverDto getDriverById(Long id, String schoolId);
    DriverDailyDetailsDto getDriverDailyDetails(Long id, String schoolId);
    DriverDto createDriver(DriverDto dto, String schoolId);
    DriverDto updateDriver(Long id, DriverDto dto, String schoolId);
    void deleteDriver(Long id, String schoolId);

    // Route & Stops Management
    List<RouteDto> getAllRoutes(String schoolId);
    RouteDto getRouteById(Long id, String schoolId);
    RouteDto createRoute(RouteDto dto, String schoolId);
    RouteDto updateRoute(Long id, RouteDto dto, String schoolId);
    void deleteRoute(Long id, String schoolId);
    List<RouteStopDto> saveRouteStops(Long routeId, List<RouteStopDto> stops, String schoolId);

    // Bus Assignments (Driver + Bus + Route + Shift)
    List<BusAssignmentDto> getAllAssignments(String schoolId);
    BusAssignmentDto createAssignment(BusAssignmentDto dto, String schoolId);
    BusAssignmentDto updateAssignment(Long id, BusAssignmentDto dto, String schoolId);
    void deleteAssignment(Long id, String schoolId);

    // Student Transport Assignments
    List<StudentAssignmentDto> getStudentAssignments(String schoolId);
    StudentAssignmentDto assignStudentToRoute(StudentAssignmentDto dto, String schoolId);
    StudentAssignmentDto updateStudentAssignment(Long id, StudentAssignmentDto dto, String schoolId);
    void removeStudentAssignment(Long id, String schoolId);
    void updateStudentPickupStatus(Long assignmentId, String status, String schoolId);

    // Transport Fees & Payments
    TransportFeeSummaryDto getFeeSummary(String schoolId);
    TransportPaymentDto collectFee(TransportPaymentDto dto, String schoolId);

    // Driver App Core Operations
    TripDtos.DriverDutyDto getDriverDuty(String driverEmail);
    TripDtos.TripSummaryDto startTrip(String driverEmail, TripDtos.StartTripRequest request);
    int ingestGpsBatch(Long tripId, String driverEmail, TripDtos.BatchGpsRequest request);
    TripDtos.StopEventDto recordStopEvent(Long tripId, Long stopId, String driverEmail, TripDtos.StopEventRequest request);
    TripDtos.TripIncidentDto reportIncident(Long tripId, String driverEmail, TripDtos.TripIncidentRequest request);
    TripDtos.TripSummaryDto endTrip(Long tripId, String driverEmail, TripDtos.EndTripRequest request);

    // Admin Live Tracking & Playback
    List<TripDtos.LiveTrackingBusDto> getLiveTracking(String schoolId);
    TripDtos.TripPlaybackDto getTripPlayback(Long tripId, String schoolId);
    List<TripDtos.TripSummaryDto> getTripHistory(String schoolId, LocalDate date, Long busId, Long driverId);

    // Alerts Center
    List<TransportAlertDto> getAlerts(String schoolId, String status);
    TransportAlertDto acknowledgeAlert(Long alertId, String userEmail, String remarks, String schoolId);
    TransportAlertDto resolveAlert(Long alertId, String userEmail, String remarks, String schoolId);

    // Parent & Student Views
    ParentTransportDto getParentChildTransport(String parentEmail, Long studentId);
    StudentTransportDto getStudentTransport(String studentEmail);

    // Demo Data Seeder for immediate rich UI testing
    void seedInitialTransportDataIfEmpty(String schoolId);
}
