package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="fee_payment_methods") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FeePaymentMethodEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=32) private String name;
    @Builder.Default private boolean active = true;
}
