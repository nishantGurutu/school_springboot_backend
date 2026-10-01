package com.school.management.dto;

import com.school.management.entity.SchoolClassEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolClassResponse {

    private Long id;
    private String name;
    private Long sectionId;
    private String section;
    private String status;

    public static SchoolClassResponse fromEntity(SchoolClassEntity entity) {
        return SchoolClassResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sectionId(entity.getSectionId())
                .section(entity.getSection())
                .status(entity.getStatus())
                .build();
    }
}
