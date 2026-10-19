package com.school.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity @Table(name="push_devices") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PushDeviceEntity {
 @Id private String id;
 private Long userId;
 @Column(length=4096) private String token;
}
