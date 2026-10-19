package com.school.management.repository;

import com.school.management.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    @Query("select m from ChatMessageEntity m where (m.senderId = :viewer and m.recipientId = :contact) or (m.senderId = :contact and m.recipientId = :viewer) order by m.id asc")
    List<ChatMessageEntity> conversation(@Param("viewer") Long viewer, @Param("contact") Long contact);
}
