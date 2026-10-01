package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "holidays", indexes = {
        @Index(name = "idx_holiday_date", columnList = "holiday_date"),
        @Index(name = "idx_holiday_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "holiday_date", nullable = false, length = 32)
    private String date;

    @Column(name = "end_date", length = 32)
    private String endDate;

    @Column(length = 64)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(length = 64)
    private String target;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (target == null || target.isBlank()) {
            target = "ALL";
        }
        if (category == null || category.isBlank()) {
            category = "School Holiday";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
