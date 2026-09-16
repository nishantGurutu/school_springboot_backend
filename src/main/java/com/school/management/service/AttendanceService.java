package com.school.management.service;

import com.school.management.dto.AttendanceRequest;
import com.school.management.dto.AttendanceResponse;
import com.school.management.entity.AttendanceEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final int MAX_PAGE_SIZE = 200;

    private final AttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAll(String type, String date, String className) {
        String t = blankToNull(type);
        String d = blankToNull(date);
        String c = blankToNull(className);

        Stream<AttendanceEntity> stream;
        if (t == null) {
            stream = attendanceRepository.findAll().stream()
                    .filter(a -> d == null || d.equals(a.getAttendanceDate()))
                    .filter(a -> c == null || c.equals(a.getClassName()));
        } else {
            if (d != null && c != null) {
                stream = attendanceRepository.findByAttendanceTypeAndAttendanceDate(t, d).stream()
                        .filter(a -> c.equals(a.getClassName()));
            } else if (d != null) {
                stream = attendanceRepository.findByAttendanceTypeAndAttendanceDate(t, d).stream();
            } else if (c != null) {
                stream = attendanceRepository.findByAttendanceTypeAndClassName(t, c).stream();
            } else {
                stream = attendanceRepository.findByAttendanceType(t).stream();
            }
        }

        return stream.map(AttendanceResponse::fromEntity).toList();
    }

    @Transactional
    public AttendanceResponse create(AttendanceRequest request) {
        String type = requireNonBlank(request.getAttendanceType(), "Attendance type is required").trim();
        String name = requireNonBlank(request.getName(), "Name is required").trim();

        AttendanceEntity entity = AttendanceEntity.builder()
                .attendanceType(type)
                .admissionNo(request.getAdmissionNo() == null ? null : request.getAdmissionNo().trim())
                .name(name)
                .rollNo(request.getRollNo() == null ? null : request.getRollNo().trim())
                .className(request.getClassName() == null ? null : request.getClassName().trim())
                .department(request.getDepartment() == null ? null : request.getDepartment().trim())
                .designation(request.getDesignation() == null ? null : request.getDesignation().trim())
                .attendanceDate(request.getAttendanceDate() == null ? null : request.getAttendanceDate().trim())
                .status(request.getStatus() == null ? null : request.getStatus().trim())
                .note(request.getNote() == null ? null : request.getNote().trim())
                .avatar(request.getAvatar())
                .build();

        return AttendanceResponse.fromEntity(attendanceRepository.save(entity));
    }

    @Transactional
    public AttendanceResponse update(Long id, AttendanceRequest request) {
        AttendanceEntity entity = findByIdOrThrow(id);
        String type = requireNonBlank(request.getAttendanceType(), "Attendance type is required").trim();
        String name = requireNonBlank(request.getName(), "Name is required").trim();

        entity.setAttendanceType(type);
        entity.setAdmissionNo(request.getAdmissionNo() == null ? entity.getAdmissionNo() : request.getAdmissionNo().trim());
        entity.setName(name);
        entity.setRollNo(request.getRollNo() == null ? entity.getRollNo() : request.getRollNo().trim());
        entity.setClassName(request.getClassName() == null ? entity.getClassName() : request.getClassName().trim());
        entity.setDepartment(request.getDepartment() == null ? entity.getDepartment() : request.getDepartment().trim());
        entity.setDesignation(request.getDesignation() == null ? entity.getDesignation() : request.getDesignation().trim());
        entity.setAttendanceDate(request.getAttendanceDate() == null ? entity.getAttendanceDate() : request.getAttendanceDate().trim());
        entity.setStatus(request.getStatus() == null ? entity.getStatus() : request.getStatus().trim());
        entity.setNote(request.getNote() == null ? entity.getNote() : request.getNote().trim());
        entity.setAvatar(request.getAvatar() == null ? entity.getAvatar() : request.getAvatar());

        return AttendanceResponse.fromEntity(attendanceRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        attendanceRepository.delete(findByIdOrThrow(id));
    }

    private AttendanceEntity findByIdOrThrow(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}