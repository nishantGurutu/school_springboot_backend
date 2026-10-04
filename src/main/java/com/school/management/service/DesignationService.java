package com.school.management.service;

import com.school.management.dto.DesignationRequest;
import com.school.management.dto.DesignationResponse;
import com.school.management.entity.DesignationEntity;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignationService {

    private final DesignationRepository designationRepository;

    @Transactional(readOnly = true)
    public List<DesignationResponse> getAll() {
        return designationRepository.findAllByOrderByIdAsc().stream()
                .map(DesignationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DesignationResponse getById(Long id) {
        DesignationEntity entity = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
        return DesignationResponse.fromEntity(entity);
    }

    @Transactional
    public DesignationResponse create(DesignationRequest request) {
        String trimmedName = request.getName().trim();
        if (designationRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Designation already exists with name: " + trimmedName);
        }

        String code = request.getCode() != null && !request.getCode().isBlank()
                ? request.getCode().trim().toUpperCase()
                : generateCodeFromName(trimmedName);

        if (code != null && !code.isBlank() && designationRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Designation code already in use: " + code);
        }

        DesignationEntity entity = DesignationEntity.builder()
                .name(trimmedName)
                .code(code)
                .category(request.getCategory() != null && !request.getCategory().isBlank()
                        ? request.getCategory().trim()
                        : "Teaching")
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .status(request.getStatus() != null && !request.getStatus().isBlank()
                        ? request.getStatus().trim()
                        : "Active")
                .build();

        DesignationEntity saved = designationRepository.save(entity);
        log.info("Created designation: {} (ID: {})", saved.getName(), saved.getId());
        return DesignationResponse.fromEntity(saved);
    }

    @Transactional
    public DesignationResponse update(Long id, DesignationRequest request) {
        DesignationEntity entity = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));

        String trimmedName = request.getName().trim();
        if (designationRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Another designation already exists with name: " + trimmedName);
        }

        String code = request.getCode() != null && !request.getCode().isBlank()
                ? request.getCode().trim().toUpperCase()
                : entity.getCode();

        if (code != null && !code.isBlank() && designationRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Another designation already uses code: " + code);
        }

        entity.setName(trimmedName);
        entity.setCode(code);
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            entity.setCategory(request.getCategory().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }

        DesignationEntity updated = designationRepository.save(entity);
        log.info("Updated designation: {} (ID: {})", updated.getName(), updated.getId());
        return DesignationResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!designationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Designation not found with id: " + id);
        }
        designationRepository.deleteById(id);
        log.info("Deleted designation with ID: {}", id);
    }

    private String generateCodeFromName(String name) {
        if (name == null || name.isBlank()) return "DSG";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].length() <= 4 ? parts[0].toUpperCase() : parts[0].substring(0, 4).toUpperCase();
        }
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) sb.append(p.charAt(0));
        }
        return sb.toString().toUpperCase();
    }
}
