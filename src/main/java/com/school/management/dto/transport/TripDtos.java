package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TripDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartTripRequest {
        private String driverEmail;
        private Long assignmentId;
        private Long routeId;
        private Long busId;
        private String shift; // MORNING, EVENING
        private Double startLatitude;
        private Double startLongitude;
        private String startLocationName;
        private Boolean preTripCheckPassed;
        private String preTripNotes;
        private Boolean chkBrakes;
        private Boolean chkLights;
        private Boolean chkTires;
        private Boolean chkEmergencyKit;
        private Boolean chkGpsActive;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GpsPointDto {
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private Double accuracyMeters;
        private LocalDateTime recordedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchGpsRequest {
        private String driverEmail;
        @Builder.Default
        private List<GpsPointDto> points = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StopEventRequest {
        private Long stopId;
        private String eventType; // ARRIVED, DEPARTED, SKIPPED
        private Double latitude;
        private Double longitude;
        private Long dwellDurationSeconds;
        private Integer studentsBoarded;
        private Integer studentsDeboarded;
        private String notes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripIncidentRequest {
        private String incidentType; // ROUTE_DEVIATION, LONG_STOP, BUS_BREAKDOWN, TRAFFIC_DELAY, ACCIDENT, EMERGENCY, OTHER
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String description;
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EndTripRequest {
        private String driverEmail;
        @Builder.Default
        private List<GpsPointDto> points = new ArrayList<>();
        private Double endLatitude;
        private Double endLongitude;
        private Double totalDistanceKm;
        private String notes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripSummaryDto {
        private Long tripId;
        private Long busId;
        private String busNumber;
        private String registrationNumber;
        private Long driverId;
        private String driverName;
        private Long routeId;
        private String routeName;
        private String routeCode;
        private String shift;
        private LocalDate tripDate;
        private String status;
        private LocalDateTime actualStartTime;
        private LocalDateTime actualEndTime;
        private Long durationMinutes;
        private Double totalDistanceKm;
        private Double currentSpeedKmh;
        private Double lastLatitude;
        private Double lastLongitude;
        private LocalDateTime lastLocationTime;
        private Integer completedStopsCount;
        private Integer totalStopsCount;
        private String currentStopName;
        private String nextStopName;
        private Integer nextStopEtaMinutes;
        private Integer incidentCount;
        private Double startLatitude;
        private Double startLongitude;
        private String startLocationName;
        private Boolean preTripCheckPassed;
        private String preTripNotes;
        private Boolean chkBrakes;
        private Boolean chkLights;
        private Boolean chkTires;
        private Boolean chkEmergencyKit;
        private Boolean chkGpsActive;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriverDutyDto {
        private Long driverId;
        private String driverName;
        private String phone;
        private String licenseNumber;
        private String licenseExpiryDate;
        private Long assignmentId;
        private Long busId;
        private String busNumber;
        private String registrationNumber;
        private Integer busCapacity;
        private Long routeId;
        private String routeName;
        private String routeCode;
        private String shift;
        private String plannedDepartureTime;
        private Long activeTripId;
        private String tripStatus; // NONE, SCHEDULED, IN_PROGRESS, COMPLETED
        private RouteDto route;
        private TripSummaryDto activeTrip;
        private Boolean todayChecklistCompleted;
        private String todayChecklistNotes;
        private Double todayStartLatitude;
        private Double todayStartLongitude;
        private String todayStartLocationName;
        private LocalDateTime todayActualStartTime;
        private Boolean todayChkBrakes;
        private Boolean todayChkLights;
        private Boolean todayChkTires;
        private Boolean todayChkEmergencyKit;
        private Boolean todayChkGpsActive;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LiveTrackingBusDto {
        private Long busId;
        private String busNumber;
        private String registrationNumber;
        private Long tripId;
        private String tripStatus;
        private Long driverId;
        private String driverName;
        private String driverPhone;
        private Long routeId;
        private String routeName;
        private String shift;
        private Double currentLatitude;
        private Double currentLongitude;
        private Double currentSpeedKmh;
        private Double heading;
        private LocalDateTime lastGpsUpdate;
        private Boolean isGpsOnline;
        private String currentStopName;
        private String nextStopName;
        private Integer nextStopEtaMinutes;
        private Integer completedStops;
        private Integer totalStops;
        private Double traveledDistanceKm;
        private Double totalRouteDistanceKm;
        private Integer openAlertsCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripPlaybackDto {
        private Long tripId;
        private String busNumber;
        private String driverName;
        private String routeName;
        private String tripDate;
        private String startTime;
        private String endTime;
        private Double totalDistanceKm;
        private Long durationMinutes;
        private List<GpsPointDto> pathPoints;
        private List<RouteStopDto> plannedStops;
        private List<StopEventDto> stopEvents;
        private List<TripIncidentDto> incidents;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StopEventDto {
        private Long id;
        private Long stopId;
        private String stopName;
        private Integer stopOrder;
        private LocalDateTime arrivalTime;
        private LocalDateTime departureTime;
        private Long dwellDurationSeconds;
        private String status;
        private Integer studentsBoarded;
        private Integer studentsDeboarded;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripIncidentDto {
        private Long id;
        private String incidentType;
        private String severity;
        private String description;
        private Double latitude;
        private Double longitude;
        private LocalDateTime reportedAt;
        private String status;
        private String adminRemarks;
    }
}
