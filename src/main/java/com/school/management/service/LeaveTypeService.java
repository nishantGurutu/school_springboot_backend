package com.school.management.service;

import com.school.management.dto.LeaveTypeRequest;
import com.school.management.dto.LeaveTypeResponse;
import com.school.management.entity.LeaveTypeEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional
    public LeaveTypeResponse create(LeaveTypeRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (leaveTypeRepository.existsByName(name)) {
            throw new DuplicateResourceException(
                    "Leave type with name '" + name + "' already exists");
        }
        LeaveTypeEntity entity = LeaveTypeEntity.builder()
                .name(name)
                .status("Active")
                .build();
        return LeaveTypeResponse.fromEntity(leaveTypeRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> getAll() {
        return leaveTypeRepository.findAll().stream()
                .map(LeaveTypeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveTypeResponse getById(Long id) {
        return LeaveTypeResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public LeaveTypeResponse update(Long id, LeaveTypeRequest request) {
        LeaveTypeEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        entity.setName(name);
        return LeaveTypeResponse.fromEntity(leaveTypeRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        LeaveTypeEntity entity = findByIdOrThrow(id);
        leaveTypeRepository.delete(entity);
    }

    private LeaveTypeEntity findByIdOrThrow(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}
