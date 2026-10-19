package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="push_devices") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PushDeviceEntity {
 @Id private String id;
 private Long userId;
 @Column(length=4096) private String token;
}
