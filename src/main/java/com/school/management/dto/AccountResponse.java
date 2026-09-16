package com.school.management.dto;

import com.school.management.entity.AccountTransactionEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

    private Long id;
    private String transactionType;
    private String category;
    private Double amount;
    private String description;
    private String transactionDate;

    public static AccountResponse fromEntity(AccountTransactionEntity transaction) {
        return AccountResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .category(transaction.getCategory())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }
}