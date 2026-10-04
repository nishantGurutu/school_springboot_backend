package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "school_designations", indexes = {
        @Index(name = "idx_desig_name", columnList = "name"),
        @Index(name = "idx_desig_code", columnList = "code"),
        @Index(name = "idx_desig_category", columnList = "category"),
        @Index(name = "idx_desig_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_desig_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(length = 32)
    private String code;

    @Builder.Default
    @Column(length = 64)
    private String category = "Teaching";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false, length = 16)
    private String status = "Active";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null || status.isBlank()) {
            status = "Active";
        }
        if (category == null || category.isBlank()) {
            category = "Teaching";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == null || status.isBlank()) {
            status = "Active";
        }
        if (category == null || category.isBlank()) {
            category = "Teaching";
        }
    }
}
