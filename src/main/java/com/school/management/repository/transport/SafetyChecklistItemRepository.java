package com.school.management.repository.transport;

import com.school.management.entity.transport.SafetyChecklistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyChecklistItemRepository extends JpaRepository<SafetyChecklistItemEntity, Long> {

    List<SafetyChecklistItemEntity> findBySchoolIdOrderBySortOrderAsc(String schoolId);

    List<SafetyChecklistItemEntity> findBySchoolIdAndIsActiveTrueOrderBySortOrderAsc(String schoolId);

    Optional<SafetyChecklistItemEntity> findByIdAndSchoolId(Long id, String schoolId);

    long countBySchoolId(String schoolId);

    void deleteBySchoolId(String schoolId);
}
