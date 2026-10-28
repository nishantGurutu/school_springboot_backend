package com.school.management.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="account_heads") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountHeadEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=16) private String transactionType;
 @Column(nullable=false,length=64) private String name;
 @Column(length=255) private String description;
 @Column(length=64) private String section;
 @Builder.Default private String status="Active";
}
