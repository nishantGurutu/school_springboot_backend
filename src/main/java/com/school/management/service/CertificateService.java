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
    private final PortalAccessService access;
    private final com.school.management.repository.StudentRepository students;

    @Transactional
    public CertificateResponse create(CertificateRequest request) {
        access.requireManager();
        var student = access.student(request.getAdmissionNo());
        String name = student.getName();
        String certName = request.getCertificateName() != null ? request.getCertificateName().trim() : null;
        if (certName != null && certificateRepository.findAll().stream().anyMatch(c -> student.getAdmissionNo().equalsIgnoreCase(c.getAdmissionNo()) && certName.equalsIgnoreCase(c.getCertificateName()))) {
            throw new DuplicateResourceException(
                    "Certificate with name '" + name + "' and certificate name '" + certName + "' already exists");
        }

        CertificateEntity entity = CertificateEntity.builder()
                .name(name)
                .admissionNo(request.getAdmissionNo())
                .rollNo(student.getRollNo())
                .className(student.getClassName())
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
                .filter(this::visible)
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse getById(Long id) {
        return CertificateResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public CertificateResponse update(Long id, CertificateRequest request) {
        access.requireManager();
        CertificateEntity entity = findByIdOrThrow(id);
        var student = access.student(request.getAdmissionNo());
        String name = student.getName();
        entity.setName(name);
        entity.setAdmissionNo(request.getAdmissionNo());
        entity.setRollNo(student.getRollNo());
        entity.setClassName(student.getClassName());
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
        access.requireManager();
        CertificateEntity entity = findByIdOrThrow(id);
        certificateRepository.delete(entity);
    }

    private CertificateEntity findByIdOrThrow(Long id) {
        return certificateRepository.findById(id).filter(this::visible)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + id));
    }

    private boolean visible(CertificateEntity c) {
        if (access.manager()) return access.actor().getRole()!=com.school.management.domain.user.Role.TEACHER || (c.getAdmissionNo()!=null && students.findByAdmissionNoIgnoreCase(c.getAdmissionNo()).map(s -> access.teaches(s.getClassName(), s.getSection())).orElse(false));
        return c.getAdmissionNo()!=null && access.children().stream().anyMatch(s -> c.getAdmissionNo().equalsIgnoreCase(s.getAdmissionNo()));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}
