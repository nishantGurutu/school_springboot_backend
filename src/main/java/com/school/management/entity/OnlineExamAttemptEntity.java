package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="online_exam_attempts", uniqueConstraints=@UniqueConstraint(columnNames={"examId","studentId"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnlineExamAttemptEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private Long version;
 private Long examId;
 private Long studentId;
 private java.time.Instant startedAt;
 private java.time.Instant deadline;
 private java.time.Instant submittedAt;
 @Column(length=100000) private String questionsJson;
 @Column(length=100000) private String answersJson;
 private Integer score;
 private Integer maximum;
}
