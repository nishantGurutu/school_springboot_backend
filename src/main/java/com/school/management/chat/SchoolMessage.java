package com.school.management.chat;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="school_chat_messages",indexes=@Index(columnList="roomId,id"),uniqueConstraints=@UniqueConstraint(columnNames={"room_id","sender_id","client_id"})) @Getter @Setter @NoArgsConstructor
public class SchoolMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="room_id",nullable=false) private String roomId;
 @Column(name="sender_id",nullable=false) private Long senderId;
 private String senderName;
 @Column(name="client_id",nullable=false) private String clientId;
 @Column(length=4000) private String text;
 private String attachmentId;
 private Instant sentAt;
}
