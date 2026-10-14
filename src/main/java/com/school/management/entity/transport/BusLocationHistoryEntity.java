package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_location_history", indexes = {
        @Index(name = "idx_loc_school", columnList = "school_id"),
        @Index(name = "idx_loc_trip", columnList = "trip_id"),
        @Index(name = "idx_loc_bus", columnList = "bus_id"),
        @Index(name = "idx_loc_time", columnList = "recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusLocationHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "bus_id", nullable = false)
    private Long busId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "speed_kmh")
    @Builder.Default
    private Double speedKmh = 0.0;

    private Double heading; // Bearing in degrees 0-360

    @Column(name = "accuracy_meters")
    @Builder.Default
    private Double accuracyMeters = 5.0;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "server_received_at", nullable = false)
    private LocalDateTime serverReceivedAt;

    @PrePersist
    void prePersist() {
        if (serverReceivedAt == null) {
            serverReceivedAt = LocalDateTime.now();
        }
        if (recordedAt == null) {
            recordedAt = serverReceivedAt;
        }
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
    }
}
