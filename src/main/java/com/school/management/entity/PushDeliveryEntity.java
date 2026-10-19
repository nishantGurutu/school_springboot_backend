package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="push_deliveries") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PushDeliveryEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long notificationId;
 private String deviceId;
 private int attempts;
 private Instant nextAttemptAt;
 private Instant deliveredAt;
 private boolean abandoned;
}
