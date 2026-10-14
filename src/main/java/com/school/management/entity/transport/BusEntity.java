package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "buses", indexes = {
        @Index(name = "idx_bus_school", columnList = "school_id"),
        @Index(name = "idx_bus_number", columnList = "bus_number"),
        @Index(name = "idx_bus_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "bus_number", nullable = false, length = 64)
    private String busNumber; // e.g. "Bus 04"

    @Column(name = "registration_number", nullable = false, unique = true, length = 64)
    private String registrationNumber; // e.g. "MH-02-AB-1234"

    @Column(nullable = false)
    private Integer capacity; // e.g. 40

    @Column(length = 64)
    private String model;

    @Column(length = 64)
    private String manufacturer;

    @Column(name = "fuel_type", length = 32)
    @Builder.Default
    private String fuelType = "DIESEL"; // DIESEL, CNG, ELECTRIC

    @Column(name = "gps_device_id", length = 64)
    private String gpsDeviceId;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    @Column(name = "fitness_expiry_date")
    private LocalDate fitnessExpiryDate;

    @Column(name = "pollution_expiry_date")
    private LocalDate pollutionExpiryDate;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, MAINTENANCE, OUT_OF_SERVICE

    // Live GPS telemetry snapshot cache
    @Column(name = "current_speed")
    @Builder.Default
    private Double currentSpeed = 0.0;

    @Column(name = "last_latitude")
    private Double lastLatitude;

    @Column(name = "last_longitude")
    private Double lastLongitude;

    @Column(name = "last_gps_update")
    private LocalDateTime lastGpsUpdate;

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
