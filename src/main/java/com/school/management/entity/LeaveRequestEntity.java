package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_leave_request_name", columnList = "name"),
        @Index(name = "idx_leave_request_leave_type", columnList = "leaveType"),
        @Index(name = "idx_leave_request_status", columnList = "status"),
        @Index(name = "idx_leave_request_user_type", columnList = "userType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applyDate;

    @Column(nullable = false)
    private String name;

    private String userType;

    private String leaveType;

    private String date;

    private String duration;

    @Builder.Default
    private String status = "Pending";

    private String reason;

    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
