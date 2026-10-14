package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_assignments", indexes = {
        @Index(name = "idx_assignment_school", columnList = "school_id"),
        @Index(name = "idx_assignment_driver", columnList = "driver_id"),
        @Index(name = "idx_assignment_bus", columnList = "bus_id"),
        @Index(name = "idx_assignment_route", columnList = "route_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "bus_id", nullable = false)
    private Long busId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String shift = "BOTH"; // MORNING, EVENING, BOTH

    @Column(name = "effective_start_date")
    private LocalDate effectiveStartDate;

    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(length = 256)
    private String notes;

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
        if (effectiveStartDate == null) {
            effectiveStartDate = LocalDate.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
