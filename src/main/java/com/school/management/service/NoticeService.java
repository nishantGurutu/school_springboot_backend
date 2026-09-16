package com.school.management.service;

import com.school.management.dto.NoticeRequest;
import com.school.management.dto.NoticeResponse;
import com.school.management.entity.NoticeEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<NoticeResponse> getAll() {
        return noticeRepository.findAll().stream().map(NoticeResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public NoticeResponse getById(Long id) {
        return NoticeResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        NoticeEntity notice = mapToEntity(new NoticeEntity(), request);
        return NoticeResponse.fromEntity(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest request) {
        NoticeEntity notice = findByIdOrThrow(id);
        return NoticeResponse.fromEntity(noticeRepository.save(mapToEntity(notice, request)));
    }

    @Transactional
    public void delete(Long id) {
        NoticeEntity notice = findByIdOrThrow(id);
        noticeRepository.delete(notice);
    }

    private NoticeEntity mapToEntity(NoticeEntity e, NoticeRequest r) {
        e.setTitle(requireNonBlank(r.getTitle(), "Title is required").trim());
        e.setDate(r.getDate());
        e.setTarget(r.getTarget());
        e.setCategory(r.getCategory());
        return e;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private NoticeEntity findByIdOrThrow(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found with id: " + id));
    }
}