package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="student_reports", indexes=@Index(name="idx_student_report_student",columnList="student_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentReportEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="student_id",nullable=false) private Long studentId;
    @Column(nullable=false,length=255) private String title;
    @Column(length=128) private String term;
    @Column(length=2000) private String description;
    @Column(nullable=false) private String storedFileName;
    private String fileName;
    private Long fileSizeBytes;
    private Long publishedBy;
    private LocalDateTime createdAt;
    @PrePersist void created() { createdAt=LocalDateTime.now(); }
}
