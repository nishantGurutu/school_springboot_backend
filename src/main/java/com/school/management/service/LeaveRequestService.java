package com.school.management.service;

import com.school.management.dto.LeaveRequestRequest;
import com.school.management.dto.LeaveRequestResponse;
import com.school.management.entity.LeaveRequestEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional
    public LeaveRequestResponse create(LeaveRequestRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        String leaveType = request.getLeaveType() != null ? request.getLeaveType().trim() : null;
        if (leaveType != null && leaveRequestRepository.existsByNameAndLeaveType(name, leaveType)) {
            throw new DuplicateResourceException(
                    "Leave request with name '" + name + "' and leave type '" + leaveType + "' already exists");
        }

        LeaveRequestEntity entity = LeaveRequestEntity.builder()
                .applyDate(request.getApplyDate())
                .name(name)
                .userType(request.getUserType())
                .leaveType(leaveType)
                .date(request.getDate())
                .duration(request.getDuration())
                .status(request.getStatus() != null ? request.getStatus().trim() : "Pending")
                .reason(request.getReason())
                .note(request.getNote())
                .build();
        return LeaveRequestResponse.fromEntity(leaveRequestRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getAll() {
        return leaveRequestRepository.findAll().stream()
                .map(LeaveRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse getById(Long id) {
        return LeaveRequestResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public LeaveRequestResponse update(Long id, LeaveRequestRequest request) {
        LeaveRequestEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        entity.setApplyDate(request.getApplyDate());
        entity.setName(name);
        entity.setUserType(request.getUserType());
        entity.setLeaveType(request.getLeaveType());
        entity.setDate(request.getDate());
        entity.setDuration(request.getDuration());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().trim());
        }
        entity.setReason(request.getReason());
        entity.setNote(request.getNote());
        return LeaveRequestResponse.fromEntity(leaveRequestRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        LeaveRequestEntity entity = findByIdOrThrow(id);
        leaveRequestRepository.delete(entity);
    }

    private LeaveRequestEntity findByIdOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}
