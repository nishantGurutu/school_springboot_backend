package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_incidents", indexes = {
        @Index(name = "idx_incident_school", columnList = "school_id"),
        @Index(name = "idx_incident_trip", columnList = "trip_id"),
        @Index(name = "idx_incident_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripIncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "bus_id")
    private Long busId;

    @Column(name = "incident_type", nullable = false, length = 64)
    private String incidentType; // ROUTE_DEVIATION, LONG_STOP, BUS_BREAKDOWN, TRAFFIC_DELAY, ACCIDENT, EMERGENCY, OTHER

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String severity = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false, length = 512)
    private String description;

    private Double latitude;
    private Double longitude;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "OPEN"; // OPEN, ACKNOWLEDGED, RESOLVED

    @Column(name = "admin_remarks", length = 512)
    private String adminRemarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (reportedAt == null) {
            reportedAt = createdAt;
        }
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
        if (status == null || status.isBlank()) {
            status = "OPEN";
        }
    }
}
