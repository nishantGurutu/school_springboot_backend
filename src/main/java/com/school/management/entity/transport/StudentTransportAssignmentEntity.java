package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_transport_assignments", indexes = {
        @Index(name = "idx_student_trans_school", columnList = "school_id"),
        @Index(name = "idx_student_trans_student", columnList = "student_id"),
        @Index(name = "idx_student_trans_route", columnList = "route_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentTransportAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 128)
    private String studentName;

    @Column(name = "admission_no", length = 32)
    private String admissionNo;

    @Column(name = "class_name", length = 64)
    private String className;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "pickup_stop_id")
    private Long pickupStopId;

    @Column(name = "drop_stop_id")
    private Long dropStopId;

    @Column(name = "pickup_time", length = 16)
    private String pickupTime;

    @Column(name = "drop_time", length = 16)
    private String dropTime;

    @Column(name = "guardian_phone", length = 32)
    private String guardianPhone;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "pickup_status", length = 32)
    @Builder.Default
    private String pickupStatus = "SCHEDULED"; // SCHEDULED, PICKED_UP, DROPPED, ABSENT

    @Column(name = "last_status_update")
    private LocalDateTime lastStatusUpdate;

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
        if (pickupStatus == null || pickupStatus.isBlank()) {
            pickupStatus = "SCHEDULED";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
