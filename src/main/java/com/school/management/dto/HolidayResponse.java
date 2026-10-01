package com.school.management.dto;

import com.school.management.entity.HolidayEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {

    private Long id;
    private String title;
    private String date;
    private String endDate;
    private String category;
    private String description;
    private String target;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HolidayResponse fromEntity(HolidayEntity entity) {
        if (entity == null) return null;
        return HolidayResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .date(entity.getDate())
                .endDate(entity.getEndDate())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .target(entity.getTarget())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
