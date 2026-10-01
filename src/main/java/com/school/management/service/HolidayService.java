package com.school.management.service;

import com.school.management.dto.HolidayRequest;
import com.school.management.dto.HolidayResponse;
import com.school.management.entity.HolidayEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.HolidayRepository;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void initDefaultHolidays() {
        try {
            if (holidayRepository.count() == 0) {
                log.info("Seeding initial school holidays into database...");
                List<HolidayEntity> initialHolidays = List.of(
                        HolidayEntity.builder()
                                .title("Gandhi Jayanti")
                                .date("2026-10-02")
                                .category("National Holiday")
                                .description("Mahatma Gandhi's birthday celebration - School closed")
                                .target("ALL")
                                .build(),
                        HolidayEntity.builder()
                                .title("Dussehra (Vijayadashami)")
                                .date("2026-10-20")
                                .endDate("2026-10-21")
                                .category("Festival")
                                .description("Dussehra festive celebration")
                                .target("ALL")
                                .build(),
                        HolidayEntity.builder()
                                .title("Diwali Break")
                                .date("2026-11-08")
                                .endDate("2026-11-10")
                                .category("Festival")
                                .description("Deepawali Festival Holidays")
                                .target("ALL")
                                .build(),
                        HolidayEntity.builder()
                                .title("Guru Nanak Jayanti")
                                .date("2026-11-24")
                                .category("Gazetted Holiday")
                                .description("Guru Nanak Gurpurab observance")
                                .target("ALL")
                                .build(),
                        HolidayEntity.builder()
                                .title("Christmas Vacation")
                                .date("2026-12-25")
                                .category("Festival")
                                .description("Christmas holiday celebration")
                                .target("ALL")
                                .build()
                );
                holidayRepository.saveAll(initialHolidays);
            }
        } catch (Exception e) {
            log.warn("Could not seed default holidays: {}", e.getMessage());
        }
    }

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
