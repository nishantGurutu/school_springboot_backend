package com.school.management.repository;

import com.school.management.entity.AccountTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransactionEntity, Long> {

    List<AccountTransactionEntity> findAllByTransactionType(String transactionType);

    boolean existsByTransactionType(String transactionType);

    long countByTransactionType(String transactionType);
}