package com.school.management.service;

import com.school.management.dto.FeesGroupRequest;
import com.school.management.dto.FeesGroupResponse;
import com.school.management.entity.FeesGroupEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.FeesGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeesGroupService {

    private static final int MAX_PAGE_SIZE = 200;

    private final FeesGroupRepository feesGroupRepository;

    @Transactional(readOnly = true)
    public List<FeesGroupResponse> getAll() {
        return feesGroupRepository.findAll().stream()
                .map(FeesGroupResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FeesGroupResponse create(FeesGroupRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (feesGroupRepository.existsByName(name)) {
            throw new DuplicateResourceException("Fees group '" + name + "' already exists");
        }

        FeesGroupEntity entity = FeesGroupEntity.builder()
                .name(name)
                .feesType(request.getFeesType() == null ? null : request.getFeesType().trim())
                .status(request.getStatus() == null ? "Active" : request.getStatus().trim())
                .build();

        return FeesGroupResponse.fromEntity(feesGroupRepository.save(entity));
    }

    @Transactional
    public FeesGroupResponse update(Long id, FeesGroupRequest request) {
        FeesGroupEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (!entity.getName().equals(name) && feesGroupRepository.existsByName(name)) {
            throw new DuplicateResourceException("Fees group '" + name + "' already exists");
        }

        entity.setName(name);
        if (request.getFeesType() != null) {
            entity.setFeesType(request.getFeesType().trim());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().trim());
        }

        return FeesGroupResponse.fromEntity(feesGroupRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        feesGroupRepository.delete(findByIdOrThrow(id));
    }

    private FeesGroupEntity findByIdOrThrow(Long id) {
        return feesGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fees group not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}