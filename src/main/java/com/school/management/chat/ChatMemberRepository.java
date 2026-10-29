package com.school.management.chat;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ChatMemberRepository extends JpaRepository<ChatMember,String> {
 List<ChatMember> findByUserId(Long userId);
 List<ChatMember> findByRoomId(String roomId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from ChatMember m where m.id = :id") Optional<ChatMember> locked(String id);
}
