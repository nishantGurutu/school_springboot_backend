package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="online_exams") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnlineExamEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private Long version;
 private String title;
 private String subject;
 private String className;
 private String section;
 private Integer durationMinutes;
 private java.time.Instant opensAt;
 private java.time.Instant closesAt;
 private boolean published;
 private Long authorUserId;
 @Column(length=100000) private String questionsJson;
}
