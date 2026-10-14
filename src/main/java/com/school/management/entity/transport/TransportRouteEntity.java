package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transport_routes", indexes = {
        @Index(name = "idx_route_school", columnList = "school_id"),
        @Index(name = "idx_route_code", columnList = "route_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportRouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "route_name", nullable = false, length = 128)
    private String routeName; // e.g. "Route 1 - North Campus Loop"

    @Column(name = "route_code", nullable = false, length = 64)
    private String routeCode; // e.g. "RT-NORTH-01"

    @Column(length = 256)
    private String description;

    @Column(name = "start_location", length = 128)
    private String startLocation;

    @Column(name = "end_location", length = 128)
    private String endLocation;

    @Column(name = "planned_duration_minutes")
    @Builder.Default
    private Integer plannedDurationMinutes = 45;

    @Column(name = "total_distance_km")
    @Builder.Default
    private Double totalDistanceKm = 18.5;

    @Column(name = "morning_start_time", length = 16)
    @Builder.Default
    private String morningStartTime = "07:15 AM";

    @Column(name = "evening_start_time", length = 16)
    @Builder.Default
    private String eveningStartTime = "02:45 PM";

    @Column(name = "speed_limit_kmh")
    @Builder.Default
    private Integer speedLimitKmh = 50;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

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
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
