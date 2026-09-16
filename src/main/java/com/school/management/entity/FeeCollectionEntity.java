package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fee_collections", indexes = {
        @Index(name = "idx_fee_collection_admission_no", columnList = "admissionNo"),
        @Index(name = "idx_fee_collection_name", columnList = "name"),
        @Index(name = "idx_fee_collection_class_name", columnList = "className"),
        @Index(name = "idx_fee_collection_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCollectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32)
    private String admissionNo;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 16)
    private String rollNo;

    @Column(length = 32)
    private String className;

    @Column(length = 16)
    private String amount;

    @Column(length = 16)
    private String paid;

    @Column(length = 16)
    private String due;

    @Column(length = 32)
    private String date;

    @Column(length = 16)
    private String status;

    @Column(length = 32)
    private String paymentType;

    @Column(length = 255)
    private String note;

    @Column(length = 255)
    private String avatar;

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