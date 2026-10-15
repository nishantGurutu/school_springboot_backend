package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles", indexes = {
        @Index(name = "idx_driver_school", columnList = "school_id"),
        @Index(name = "idx_driver_email", columnList = "email"),
        @Index(name = "idx_driver_phone", columnList = "phone"),
        @Index(name = "idx_driver_license", columnList = "license_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 128)
    private String email;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(name = "license_number", nullable = false, length = 64)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "license_category", length = 32)
    @Builder.Default
    private String licenseCategory = "COMMERCIAL_HEAVY"; // COMMERCIAL_HEAVY, MEDIUM, LIGHT

    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 5;

    @Column(name = "verification_status", nullable = false, length = 32)
    @Builder.Default
    private String verificationStatus = "VERIFIED"; // PENDING, VERIFIED, REJECTED

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, ON_LEAVE

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "emergency_contact", length = 64)
    private String emergencyContact;

    @Column(length = 256)
    private String address;

    @Column(name = "assigned_bus_id")
    private Long assignedBusId;

    @Column(name = "assigned_route_id")
    private Long assignedRouteId;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

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
        if (verificationStatus == null || verificationStatus.isBlank()) {
            verificationStatus = "VERIFIED";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
