package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_issues", indexes = {
        @Index(name = "idx_book_issue_card_no", columnList = "card_no"),
        @Index(name = "idx_book_issue_book_name", columnList = "book_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_no", length = 64)
    private String cardNo;

    @Column(name = "issue_to", nullable = false, length = 128)
    private String issueTo;

    @Column(length = 64)
    private String className;

    @Column(name = "book_name", nullable = false, length = 255)
    private String bookName;

    @Column(length = 64)
    private String number;

    @Column(length = 32)
    private String issueDate;

    @Column(length = 32)
    private String returnDate;

    @Builder.Default
    @Column(nullable = false, length = 16)
    private String status = "Issued";

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