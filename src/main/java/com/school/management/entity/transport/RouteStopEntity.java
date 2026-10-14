package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_stops", indexes = {
        @Index(name = "idx_stop_school", columnList = "school_id"),
        @Index(name = "idx_stop_route", columnList = "route_id"),
        @Index(name = "idx_stop_order", columnList = "stop_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "stop_name", nullable = false, length = 128)
    private String stopName;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "planned_morning_pickup_time", length = 16)
    private String plannedMorningPickupTime; // e.g. "07:25 AM"

    @Column(name = "planned_evening_drop_time", length = 16)
    private String plannedEveningDropTime; // e.g. "03:15 PM"

    @Column(name = "geofence_radius_meters")
    @Builder.Default
    private Integer geofenceRadiusMeters = 50;

    @Column(length = 256)
    private String landmark;

    @Column(name = "assigned_students_count")
    @Builder.Default
    private Integer assignedStudentsCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
        if (geofenceRadiusMeters == null || geofenceRadiusMeters <= 0) {
            geofenceRadiusMeters = 50;
        }
    }
}
