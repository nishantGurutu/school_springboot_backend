package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="fee_payments") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FeePaymentEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long feeId;
 @Column(unique=true) private String providerId;
 private String reference;
 private String checkoutUrl;
 private long amountMinor;
 private long paidBeforeMinor;
 private String status;
 private String receipt;
 private Instant createdAt;
 private Instant verifiedAt;
}
