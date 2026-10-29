package com.school.management.repository;
import com.school.management.entity.PortalNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PortalNotificationRepository extends JpaRepository<PortalNotificationEntity,Long> {
 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query("update PortalNotificationEntity n set n.readAt = :now where n.userId = :userId and n.route = 'chat' and n.referenceId = :room and n.readAt is null and n.chatMessageId <= :through")
 int markChatRead(Long userId,String room,Long through,java.time.LocalDateTime now);
 List<PortalNotificationEntity> findByUserIdOrderByIdDesc(Long userId); }
