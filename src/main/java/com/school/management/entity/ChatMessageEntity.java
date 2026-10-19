package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_sender", columnList = "senderId"),
    @Index(name = "idx_chat_recipient", columnList = "recipientId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long senderId;
    @Column(nullable = false) private Long recipientId;
    @Column(nullable = false, length = 4000) private String content;
    @Column(nullable = false) private LocalDateTime sentAt;
}
