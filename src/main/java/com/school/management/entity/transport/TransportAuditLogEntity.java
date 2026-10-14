package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transport_audit_logs", indexes = {
        @Index(name = "idx_audit_school", columnList = "school_id"),
        @Index(name = "idx_audit_actor", columnList = "actor_user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_time", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 128)
    private String actorEmail;

    @Column(name = "actor_role", length = 64)
    private String actorRole;

    @Column(nullable = false, length = 64)
    private String action; // e.g. "START_TRIP", "END_TRIP", "BUS_ASSIGNMENT_CHANGED", "GPS_BATCH_UPLOAD"

    @Column(name = "resource_type", length = 64)
    private String resourceType; // BUS, DRIVER, ROUTE, TRIP, ALERT

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String result = "SUCCESS"; // SUCCESS, DENIED, FAILED

    @Column(length = 1024)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
    }
}
