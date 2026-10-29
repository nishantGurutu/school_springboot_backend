package com.school.management.chat;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="school_chat_members",indexes=@Index(columnList="userId")) @Getter @Setter @NoArgsConstructor
public class ChatMember {
 @Id private String id;
 private String roomId;
 private Long userId;
 private long deliveredThrough;
 private long readThrough;
}
