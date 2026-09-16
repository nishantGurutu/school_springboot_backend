package com.school.management.service;

import com.school.management.dto.FeeCollectionRequest;
import com.school.management.dto.FeeCollectionResponse;
import com.school.management.entity.FeeCollectionEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.FeeCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeCollectionService {

    private static final int MAX_PAGE_SIZE = 200;

    private final FeeCollectionRepository feeCollectionRepository;

    @Transactional(readOnly = true)
    public List<FeeCollectionResponse> getAll() {
        return feeCollectionRepository.findAll().stream()
                .map(FeeCollectionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FeeCollectionResponse create(FeeCollectionRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();

        FeeCollectionEntity entity = FeeCollectionEntity.builder()
                .admissionNo(request.getAdmissionNo() == null ? null : request.getAdmissionNo().trim())
                .name(name)
                .rollNo(request.getRollNo() == null ? null : request.getRollNo().trim())
                .className(request.getClassName() == null ? null : request.getClassName().trim())
                .amount(request.getAmount() == null ? null : request.getAmount().trim())
                .paid(request.getPaid() == null ? null : request.getPaid().trim())
                .due(request.getDue() == null ? null : request.getDue().trim())
                .date(request.getDate() == null ? null : request.getDate().trim())
                .status(request.getStatus() == null ? null : request.getStatus().trim())
                .paymentType(request.getPaymentType() == null ? null : request.getPaymentType().trim())
                .note(request.getNote() == null ? null : request.getNote().trim())
                .avatar(request.getAvatar())
                .build();

        return FeeCollectionResponse.fromEntity(feeCollectionRepository.save(entity));
    }

    @Transactional
    public FeeCollectionResponse update(Long id, FeeCollectionRequest request) {
        FeeCollectionEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();

        entity.setAdmissionNo(request.getAdmissionNo() == null ? entity.getAdmissionNo() : request.getAdmissionNo().trim());
        entity.setName(name);
        entity.setRollNo(request.getRollNo() == null ? entity.getRollNo() : request.getRollNo().trim());
        entity.setClassName(request.getClassName() == null ? entity.getClassName() : request.getClassName().trim());
        entity.setAmount(request.getAmount() == null ? entity.getAmount() : request.getAmount().trim());
        entity.setPaid(request.getPaid() == null ? entity.getPaid() : request.getPaid().trim());
        entity.setDue(request.getDue() == null ? entity.getDue() : request.getDue().trim());
        entity.setDate(request.getDate() == null ? entity.getDate() : request.getDate().trim());
        entity.setStatus(request.getStatus() == null ? entity.getStatus() : request.getStatus().trim());
        entity.setPaymentType(request.getPaymentType() == null ? entity.getPaymentType() : request.getPaymentType().trim());
        entity.setNote(request.getNote() == null ? entity.getNote() : request.getNote().trim());
        entity.setAvatar(request.getAvatar() == null ? entity.getAvatar() : request.getAvatar());

        return FeeCollectionResponse.fromEntity(feeCollectionRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        feeCollectionRepository.delete(findByIdOrThrow(id));
    }

    private FeeCollectionEntity findByIdOrThrow(Long id) {
        return feeCollectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee collection not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}