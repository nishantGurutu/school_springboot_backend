package com.school.management.chat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ChatRoomRepository extends JpaRepository<ChatRoom,String> { @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select r from ChatRoom r where r.id = :id") Optional<ChatRoom> locked(String id);
 Optional<ChatRoom> findBySchoolIdAndDirectKey(String schoolId,String directKey); }
