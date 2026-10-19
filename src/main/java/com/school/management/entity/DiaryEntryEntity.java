package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="student_diary") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiaryEntryEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String title;
 @Column(length=12000) private String content;
 private String className;
 private String section;
 private Long targetStudentId;
 private Long authorUserId;
 private String fileUrl;
 private java.time.LocalDateTime postedAt;
}
