package com.school.management.repository;

import com.school.management.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiryDate < :now OR r.revoked = true")
    void deleteExpiredOrRevoked(@Param("now") LocalDateTime now);
}
