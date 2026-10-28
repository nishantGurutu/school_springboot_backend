package com.school.management.repository;
import com.school.management.entity.FeePaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FeePaymentMethodRepository extends JpaRepository<FeePaymentMethodEntity,Long> {
    List<FeePaymentMethodEntity> findByActiveTrueOrderByIdAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameAndActiveTrue(String name);
}
