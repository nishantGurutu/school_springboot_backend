package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework_submissions", uniqueConstraints = @UniqueConstraint(columnNames = {"homework_id", "student_id"}))
@Getter @Setter @NoArgsConstructor
public class HomeworkSubmissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(nullable = false)
    private LocalDateTime submittedAt;
}
