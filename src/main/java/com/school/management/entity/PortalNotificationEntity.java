package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="portal_notifications") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalNotificationEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long userId;
 private String title;
 @Column(length=12000) private String body;
 private String route;
 private String referenceId;
 private java.time.LocalDateTime createdAt;
 private java.time.LocalDateTime readAt;
}
