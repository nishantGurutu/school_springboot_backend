package com.school.management.repository;

import com.school.management.entity.PushDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PushDeliveryRepository extends JpaRepository<PushDeliveryEntity,Long>{
 List<PushDeliveryEntity> findTop50ByDeliveredAtIsNullAndAbandonedFalseAndNextAttemptAtBeforeOrderByIdAsc(Instant now);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select d from PushDeliveryEntity d where d.id=:id")
 Optional<PushDeliveryEntity> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
