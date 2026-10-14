package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transport_alerts", indexes = {
        @Index(name = "idx_alert_school", columnList = "school_id"),
        @Index(name = "idx_alert_trip", columnList = "trip_id"),
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_type", columnList = "alert_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "bus_id")
    private Long busId;

    @Column(name = "bus_number", length = 64)
    private String busNumber;

    @Column(name = "alert_type", nullable = false, length = 64)
    private String alertType; // GPS_OFFLINE, ROUTE_DEVIATION, LONG_STOP, OVERSPEED, DELAY, LICENCE_EXPIRY, BREAKDOWN, MISSED_STOP

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String severity = "WARNING"; // INFO, WARNING, CRITICAL

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 512)
    private String message;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "OPEN"; // OPEN, ACKNOWLEDGED, RESOLVED

    @Column(name = "acknowledged_by", length = 128)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_remarks", length = 512)
    private String resolutionRemarks;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
        if (status == null || status.isBlank()) {
            status = "OPEN";
        }
    }
}
