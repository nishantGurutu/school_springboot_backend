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

        return FeesDiscountResponse.fromEntity(feesDiscountRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        feesDiscountRepository.delete(findByIdOrThrow(id));
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