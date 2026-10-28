package com.school.management.repository;
import com.school.management.entity.AccountHeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountHeadRepository extends JpaRepository<AccountHeadEntity,Long> {
 boolean existsByTransactionTypeAndNameIgnoreCase(String type,String name);
}
