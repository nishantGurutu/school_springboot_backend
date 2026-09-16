package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_book_number", columnList = "book_number"),
        @Index(name = "idx_book_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128)
    private String subject;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 128)
    private String publisher;

    @Column(length = 128)
    private String author;

    @Column(name = "book_number", unique = true, length = 64)
    private String bookNumber;

    @Column(length = 64)
    private String rackNo;

    private Integer qty;

    private Integer available;

    @Column(length = 32)
    private String price;

    @Column(length = 32)
    private String date;

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