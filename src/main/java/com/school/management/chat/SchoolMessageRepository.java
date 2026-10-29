package com.school.management.chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface SchoolMessageRepository extends JpaRepository<SchoolMessage,Long> {
 List<SchoolMessage> findByRoomIdAndIdGreaterThanOrderByIdAsc(String roomId,Long after,Pageable page);
 Optional<SchoolMessage> findByRoomIdAndSenderIdAndClientId(String roomId,Long senderId,String clientId);
 Optional<SchoolMessage> findFirstByRoomIdOrderByIdDesc(String roomId);
 long countByRoomIdAndIdGreaterThanAndSenderIdNot(String roomId,Long after,Long userId);
}
