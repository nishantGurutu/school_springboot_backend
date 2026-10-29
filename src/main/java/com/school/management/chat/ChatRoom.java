package com.school.management.chat;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="school_chat_rooms", uniqueConstraints=@UniqueConstraint(columnNames={"school_id","direct_key"}))
@Getter @Setter @NoArgsConstructor
public class ChatRoom {
 @Id private String id;
 @Column(name="school_id",nullable=false) private String schoolId;
 @Column(name="direct_key") private String directKey;
 private String name;
 private boolean groupChat;
 private Long ownerId;
 private Instant createdAt;
}
