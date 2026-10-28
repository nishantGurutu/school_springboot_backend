package com.school.management.controller;
import com.school.management.entity.FeePaymentMethodEntity;
import com.school.management.repository.FeePaymentMethodRepository;
import com.school.management.exceptions.DuplicateResourceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/fees/payment-types") @RequiredArgsConstructor
public class FeePaymentMethodController {
    private final FeePaymentMethodRepository methods;
    public record CreatePaymentType(@NotBlank(message="Payment type name is required")
                                    @Size(max=32, message="Payment type name must be at most 32 characters") String name) {}
    @GetMapping public List<FeePaymentMethodEntity> list() { return methods.findByActiveTrueOrderByIdAsc(); }
    @PostMapping @Transactional
    public ResponseEntity<FeePaymentMethodEntity> create(@Valid @RequestBody CreatePaymentType request) {
        String name = request.name().trim().replaceAll("\\s+", " ");
        if (methods.existsByNameIgnoreCase(name)) throw new DuplicateResourceException("This payment type already exists. Select it from the dropdown.");
        var saved = methods.save(FeePaymentMethodEntity.builder().name(name).active(true).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
