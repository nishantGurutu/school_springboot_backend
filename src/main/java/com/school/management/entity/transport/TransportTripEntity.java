package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transport_trips", indexes = {
        @Index(name = "idx_trip_school", columnList = "school_id"),
        @Index(name = "idx_trip_driver", columnList = "driver_id"),
        @Index(name = "idx_trip_bus", columnList = "bus_id"),
        @Index(name = "idx_trip_route", columnList = "route_id"),
        @Index(name = "idx_trip_status", columnList = "status"),
        @Index(name = "idx_trip_date", columnList = "trip_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportTripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "bus_assignment_id")
    private Long busAssignmentId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "bus_id", nullable = false)
    private Long busId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String shift = "MORNING"; // MORNING, EVENING

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "SCHEDULED"; // SCHEDULED, PRE_TRIP_CHECK, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "total_distance_km")
    @Builder.Default
    private Double totalDistanceKm = 0.0;

    @Column(name = "current_speed_kmh")
    @Builder.Default
    private Double currentSpeedKmh = 0.0;

    @Column(name = "last_latitude")
    private Double lastLatitude;

    @Column(name = "last_longitude")
    private Double lastLongitude;

    @Column(name = "last_location_time")
    private LocalDateTime lastLocationTime;

    @Column(name = "current_stop_id")
    private Long currentStopId;

    @Column(name = "next_stop_id")
    private Long nextStopId;

    @Column(name = "next_stop_eta_minutes")
    private Integer nextStopEtaMinutes;

    @Column(name = "completed_stops_count")
    @Builder.Default
    private Integer completedStopsCount = 0;

    @Column(name = "total_stops_count")
    @Builder.Default
    private Integer totalStopsCount = 0;

    @Column(name = "pre_trip_check_passed")
    @Builder.Default
    private Boolean preTripCheckPassed = false;

    @Column(name = "pre_trip_notes", length = 512)
    private String preTripNotes;

    @Column(name = "end_notes", length = 512)
    private String endNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
        if (tripDate == null) {
            tripDate = LocalDate.now();
        }
        if (status == null || status.isBlank()) {
            status = "SCHEDULED";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
