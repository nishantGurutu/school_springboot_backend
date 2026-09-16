package com.school.management.service;

import com.school.management.dto.FeesTypeRequest;
import com.school.management.dto.FeesTypeResponse;
import com.school.management.entity.FeesTypeEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.FeesTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeesTypeService {

    private static final int MAX_PAGE_SIZE = 200;

    private final FeesTypeRepository feesTypeRepository;

    @Transactional(readOnly = true)
    public List<FeesTypeResponse> getAll() {
        return feesTypeRepository.findAll().stream()
                .map(FeesTypeResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FeesTypeResponse create(FeesTypeRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (feesTypeRepository.existsByName(name)) {
            throw new DuplicateResourceException("Fees type '" + name + "' already exists");
        }

        FeesTypeEntity entity = FeesTypeEntity.builder()
                .name(name)
                .status(request.getStatus() == null ? "Active" : request.getStatus().trim())
                .build();

        return FeesTypeResponse.fromEntity(feesTypeRepository.save(entity));
    }

    @Transactional
    public FeesTypeResponse update(Long id, FeesTypeRequest request) {
        FeesTypeEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (!entity.getName().equals(name) && feesTypeRepository.existsByName(name)) {
            throw new DuplicateResourceException("Fees type '" + name + "' already exists");
        }

        entity.setName(name);
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().trim());
        }

        return FeesTypeResponse.fromEntity(feesTypeRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        feesTypeRepository.delete(findByIdOrThrow(id));
    }

    private FeesTypeEntity findByIdOrThrow(Long id) {
        return feesTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fees type not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}