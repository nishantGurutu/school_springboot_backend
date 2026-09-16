package com.school.management.service;

import com.school.management.dto.CertificateRequest;
import com.school.management.dto.CertificateResponse;
import com.school.management.entity.CertificateEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;

    @Transactional
    public CertificateResponse create(CertificateRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        String certName = request.getCertificateName() != null ? request.getCertificateName().trim() : null;
        if (certName != null && certificateRepository.existsByNameAndCertificateName(name, certName)) {
            throw new DuplicateResourceException(
                    "Certificate with name '" + name + "' and certificate name '" + certName + "' already exists");
        }

        CertificateEntity entity = CertificateEntity.builder()
                .name(name)
                .rollNo(request.getRollNo())
                .className(request.getClassName())
                .certificateName(certName)
                .bgImage(request.getBgImage())
                .avatar(request.getAvatar())
                .date(request.getDate())
                .footerLeft(request.getFooterLeft())
                .footerRight(request.getFooterRight())
                .build();
        return CertificateResponse.fromEntity(certificateRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getAll() {
        return certificateRepository.findAll().stream()
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse getById(Long id) {
        return CertificateResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public CertificateResponse update(Long id, CertificateRequest request) {
        CertificateEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        entity.setName(name);
        entity.setRollNo(request.getRollNo());
        entity.setClassName(request.getClassName());
        entity.setCertificateName(request.getCertificateName());
        entity.setBgImage(request.getBgImage());
        entity.setAvatar(request.getAvatar());
        entity.setDate(request.getDate());
        entity.setFooterLeft(request.getFooterLeft());
        entity.setFooterRight(request.getFooterRight());
        return CertificateResponse.fromEntity(certificateRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        CertificateEntity entity = findByIdOrThrow(id);
        certificateRepository.delete(entity);
    }

    private CertificateEntity findByIdOrThrow(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}
