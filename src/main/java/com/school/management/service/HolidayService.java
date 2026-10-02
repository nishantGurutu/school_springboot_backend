package com.school.management.service;

import com.school.management.dto.HolidayRequest;
import com.school.management.dto.HolidayResponse;
import com.school.management.entity.HolidayEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<HolidayResponse> getAll() {
        return holidayRepository.findAllByOrderByDateAsc().stream()
                .map(HolidayResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public HolidayResponse getById(Long id) {
        return HolidayResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        HolidayEntity holiday = mapToEntity(new HolidayEntity(), request);
        return HolidayResponse.fromEntity(holidayRepository.save(holiday));
    }

    @Transactional
    public HolidayResponse update(Long id, HolidayRequest request) {
        HolidayEntity holiday = findByIdOrThrow(id);
        return HolidayResponse.fromEntity(holidayRepository.save(mapToEntity(holiday, request)));
    }

    @Transactional
    public void delete(Long id) {
        HolidayEntity holiday = findByIdOrThrow(id);
        holidayRepository.delete(holiday);
    }

    @Transactional(readOnly = true)
    public Optional<HolidayEntity> getHolidayForDate(String date) {
        if (date == null || date.isBlank()) return Optional.empty();
        List<HolidayEntity> matching = holidayRepository.findHolidaysCoveringDate(date.trim());
        if (!matching.isEmpty()) {
            return Optional.of(matching.get(0));
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(String date) {
        return getHolidayForDate(date).isPresent();
    }

    private HolidayEntity mapToEntity(HolidayEntity e, HolidayRequest r) {
        e.setTitle(requireNonBlank(r.getTitle(), "Holiday title is required").trim());
        e.setDate(requireNonBlank(r.getDate(), "Holiday date is required").trim());
        e.setEndDate(r.getEndDate() != null && !r.getEndDate().isBlank() ? r.getEndDate().trim() : null);
        e.setCategory(r.getCategory() != null && !r.getCategory().isBlank() ? r.getCategory().trim() : "School Holiday");
        e.setDescription(r.getDescription() != null ? r.getDescription().trim() : "");
        e.setTarget(r.getTarget() != null && !r.getTarget().isBlank() ? r.getTarget().trim() : "ALL");
        return e;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private HolidayEntity findByIdOrThrow(Long id) {
        return holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
    }
}
