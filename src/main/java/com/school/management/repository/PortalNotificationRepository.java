package com.school.management.repository;
import com.school.management.entity.PortalNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PortalNotificationRepository extends JpaRepository<PortalNotificationEntity,Long> { List<PortalNotificationEntity> findByUserIdOrderByIdDesc(Long userId); }
