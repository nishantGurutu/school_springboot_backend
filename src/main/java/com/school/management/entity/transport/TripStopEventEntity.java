package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_stop_events", indexes = {
        @Index(name = "idx_stop_event_school", columnList = "school_id"),
        @Index(name = "idx_stop_event_trip", columnList = "trip_id"),
        @Index(name = "idx_stop_event_stop", columnList = "stop_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripStopEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "stop_id")
    private Long stopId;

    @Column(name = "stop_name", nullable = false, length = 128)
    private String stopName;

    @Column(name = "stop_order")
    private Integer stopOrder;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "dwell_duration_seconds")
    @Builder.Default
    private Long dwellDurationSeconds = 0L;

    @Column(name = "is_planned")
    @Builder.Default
    private Boolean isPlanned = true;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ARRIVED"; // ARRIVED, DEPARTED, SKIPPED

    @Column(name = "students_boarded_count")
    @Builder.Default
    private Integer studentsBoardedCount = 0;

    @Column(name = "students_deboarded_count")
    @Builder.Default
    private Integer studentsDeboardedCount = 0;

    @Column(length = 256)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
        if (status == null || status.isBlank()) {
            status = "ARRIVED";
        }
    }
}
