package com.school.management.service;

import com.school.management.dto.CertificateRequest;
import com.school.management.dto.CertificateResponse;
import com.school.management.entity.CertificateEntity;
import com.school.management.entity.StudentEntity;
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
        String certName = requireNonBlank(request.getCertificateName(), "Certificate name is required").trim();
        if (certName != null && certificateRepository.findAll().stream().anyMatch(c -> student.getAdmissionNo().equalsIgnoreCase(c.getAdmissionNo()) && certName.equalsIgnoreCase(c.getCertificateName()))) {
            throw new DuplicateResourceException(
                    "Certificate with name '" + name + "' and certificate name '" + certName + "' already exists");
        }

        CertificateEntity entity = CertificateEntity.builder()
                .name(name)
                .admissionNo(student.getAdmissionNo())
                .rollNo(student.getRollNo())
                .className(certificateClass(student))
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
    public List<CertificateResponse> getAll(String admissionNo) {
        return certificateRepository.findAll().stream()
                .filter(this::visible)
                .filter(c -> admissionNo == null || admissionNo.isBlank() || admissionNo.trim().equalsIgnoreCase(c.getAdmissionNo()))
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse getById(Long id) {
        return CertificateResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CertificateEntity getEntityById(Long id) {
        return findByIdOrThrow(id);
    }

    @Transactional
    public CertificateResponse update(Long id, CertificateRequest request) {
        access.requireManager();
        CertificateEntity entity = findByIdOrThrow(id);
        var student = access.student(request.getAdmissionNo());
        String name = student.getName();
        entity.setName(name);
        entity.setAdmissionNo(student.getAdmissionNo());
        entity.setRollNo(student.getRollNo());
        entity.setClassName(certificateClass(student));
        entity.setCertificateName(requireNonBlank(request.getCertificateName(), "Certificate name is required").trim());
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
        if (access.manager()) {
            if (access.actor().getRole() != com.school.management.domain.user.Role.TEACHER) return true;
            if (c.getAdmissionNo() != null && !c.getAdmissionNo().isBlank()) {
                return students.findByAdmissionNoIgnoreCase(c.getAdmissionNo())
                        .map(s -> access.teaches(s.getClassName(), s.getSection()))
                        .orElse(true);
            }
            return true;
        }
        if (c.getAdmissionNo() == null || c.getAdmissionNo().isBlank()) return false;
        return access.children().stream().anyMatch(s ->
                c.getAdmissionNo().equalsIgnoreCase(s.getAdmissionNo()));
    }

    private String certificateClass(StudentEntity student) {
        return student.getClassName() + (student.getSection() == null || student.getSection().isBlank()
                ? "" : " (" + student.getSection() + ")");
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }
}
