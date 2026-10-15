package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework_submissions", uniqueConstraints = @UniqueConstraint(columnNames = {"homework_id", "student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HomeworkSubmissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    private String studentName;

    private String studentRollNo;

    private String studentAdmissionNo;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(columnDefinition = "TEXT")
    private String submissionText;

    private String fileUrl;

    @Builder.Default
    private String status = "SUBMITTED"; // SUBMITTED, ACCEPTED, REJECTED, RESUBMITTED, GRADED

    private Integer points; // scored marks

    @Column(columnDefinition = "TEXT")
    private String teacherFeedback;

    private LocalDateTime reviewedAt;
}
