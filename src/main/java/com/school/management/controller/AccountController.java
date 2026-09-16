package com.school.management.controller;

import com.school.management.dto.AccountRequest;
import com.school.management.dto.AccountResponse;
import com.school.management.response.ApiResponse;
import com.school.management.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Accounts and finance management APIs")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/summary")
    @Operation(summary = "Get accounts summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok("summary", accountService.getSummary()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List all transactions")
    public ResponseEntity<List<AccountResponse>> getAll() {
        return ResponseEntity.ok(accountService.getAll());
    }

    @PostMapping("/transactions")
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @DeleteMapping("/transactions/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}