package com.school.management.service;

import com.school.management.dto.FeesDiscountRequest;
import com.school.management.dto.FeesDiscountResponse;
import com.school.management.entity.FeesDiscountEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.FeesDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeesDiscountService {

    private static final int MAX_PAGE_SIZE = 200;

    private final FeesDiscountRepository feesDiscountRepository;
    private final com.school.management.repository.FeesTypeRepository feesTypes;

    @Transactional(readOnly = true)
    public List<FeesDiscountResponse> getAll() {
        return feesDiscountRepository.findAll().stream()
                .map(FeesDiscountResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FeesDiscountResponse create(FeesDiscountRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (feesDiscountRepository.existsByName(name)) {
            throw new DuplicateResourceException("Fees discount '" + name + "' already exists");
        }

        FeesDiscountEntity entity = FeesDiscountEntity.builder()
                .name(name)
                .feesType(request.getFeesType() == null ? null : request.getFeesType().trim())
                .discountType(request.getDiscountType() == null ? null : request.getDiscountType().trim())
                .discountValue(request.getDiscountValue() == null ? null : request.getDiscountValue().trim())
                .status(request.getStatus() == null ? "Active" : request.getStatus().trim())
                .build();

        validate(entity);
        return FeesDiscountResponse.fromEntity(feesDiscountRepository.save(entity));
    }

    @Transactional
    public FeesDiscountResponse update(Long id, FeesDiscountRequest request) {
        FeesDiscountEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (!entity.getName().equals(name) && feesDiscountRepository.existsByName(name)) {
            throw new DuplicateResourceException("Fees discount '" + name + "' already exists");
        }

        entity.setName(name);
        if (request.getFeesType() != null) {
            entity.setFeesType(request.getFeesType().trim());
        }
        if (request.getDiscountType() != null) {
            entity.setDiscountType(request.getDiscountType().trim());
        }
        if (request.getDiscountValue() != null) {
            entity.setDiscountValue(request.getDiscountValue().trim());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().trim());
        }

        validate(entity);
        return FeesDiscountResponse.fromEntity(feesDiscountRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        feesDiscountRepository.delete(findByIdOrThrow(id));
    }

    private void validate(FeesDiscountEntity entity) {
        String scope = entity.getFeesType();
        if (scope == null || scope.isBlank() || "All".equalsIgnoreCase(scope.trim())) entity.setFeesType("All");
        else {
            var names = java.util.Arrays.stream(scope.split(",")).map(String::trim).toList();
            var activeTypes = feesTypes.findAll().stream().filter(t -> "Active".equalsIgnoreCase(t.getStatus())).toList();
            if (names.stream().anyMatch(name -> activeTypes.stream().noneMatch(t -> t.getName().equalsIgnoreCase(name))))
                throw new BadRequestException("Select an active applicable fee type or All fee types");
            entity.setFeesType(String.join(", ", names));
        }
        String type = requireNonBlank(entity.getDiscountType(), "Discount type is required").trim();
        if (!type.equals("Percentage") && !type.equals("Fixed Amount")) throw new BadRequestException("Select Percentage or Fixed Amount");
        String raw = requireNonBlank(entity.getDiscountValue(), "Discount value is required").trim();
        if (type.equals("Percentage") && raw.endsWith("%")) raw = raw.substring(0, raw.length()-1).trim();
        java.math.BigDecimal value;
        try { value = new java.math.BigDecimal(raw); } catch (Exception e) { throw new BadRequestException("Enter a valid discount value"); }
        if (value.signum() < 0 || (type.equals("Percentage") && value.compareTo(new java.math.BigDecimal("100")) > 0))
            throw new BadRequestException("Discount value must be non-negative; percentage cannot exceed 100");
        if (value.scale() > 2) throw new BadRequestException("Discount value can have at most two decimal places");
        entity.setDiscountType(type); entity.setDiscountValue(value.stripTrailingZeros().toPlainString());
        String status = entity.getStatus();
        if (status == null || status.isBlank()) entity.setStatus("Active");
        else if (status.equalsIgnoreCase("Active")) entity.setStatus("Active");
        else if (status.equalsIgnoreCase("Inactive")) entity.setStatus("Inactive");
        else throw new BadRequestException("Select Active or Inactive status");
    }

    private FeesDiscountEntity findByIdOrThrow(Long id) {
        return feesDiscountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fees discount not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}