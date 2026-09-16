package com.school.management.service;

import com.school.management.dto.AccountRequest;
import com.school.management.dto.AccountResponse;
import com.school.management.entity.AccountTransactionEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.AccountTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountTransactionRepository accountTransactionRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {
        List<AccountTransactionEntity> incomes = accountTransactionRepository.findAllByTransactionType("INCOME");
        List<AccountTransactionEntity> expenses = accountTransactionRepository.findAllByTransactionType("EXPENSE");
        Double totalIncome = sumOf(incomes);
        Double totalExpenses = sumOf(expenses);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpenses);
        summary.put("netBalance", totalIncome - totalExpenses);
        summary.put("incomeCount", incomes.size());
        summary.put("expenseCount", expenses.size());
        return summary;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAll() {
        return accountTransactionRepository.findAll().stream().map(AccountResponse::fromEntity).toList();
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        String type = requireNonBlank(request.getTransactionType(), "Transaction type is required").trim().toUpperCase();
        if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
            throw new BadRequestException(
                    "Invalid transaction type '" + request.getTransactionType() + "'. Allowed values: INCOME, EXPENSE");
        }
        AccountTransactionEntity transaction = mapToEntity(new AccountTransactionEntity(), request);
        transaction.setTransactionType(type);
        return AccountResponse.fromEntity(accountTransactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long id) {
        AccountTransactionEntity transaction = findByIdOrThrow(id);
        accountTransactionRepository.delete(transaction);
    }

    private AccountTransactionEntity mapToEntity(AccountTransactionEntity e, AccountRequest r) {
        e.setCategory(r.getCategory());
        e.setAmount(r.getAmount());
        e.setDescription(r.getDescription());
        e.setTransactionDate(r.getTransactionDate());
        return e;
    }

    private Double sumOf(List<AccountTransactionEntity> transactions) {
        return transactions.stream()
                .mapToDouble(t -> t.getAmount() == null ? 0.0 : t.getAmount())
                .sum();
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private AccountTransactionEntity findByIdOrThrow(Long id) {
        return accountTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }
}