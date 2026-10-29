package com.school.management.chat;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="school_chat_attachments") @Getter @Setter @NoArgsConstructor
public class ChatAttachment {
 @Id private String id;
 private String roomId;
 private Long uploaderId;
 private String fileName;
 private String contentType;
 private long size;
 private String storageKey;
}
