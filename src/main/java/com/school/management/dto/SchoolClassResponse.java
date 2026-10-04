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
    private String label;

    public static SchoolClassResponse fromEntity(SchoolClassEntity entity) {
        if (entity == null) return null;
        String sec = entity.getSection() != null ? entity.getSection().trim() : "";
        String lbl = sec.isEmpty() ? entity.getName() : entity.getName() + " (" + sec + ")";
        return SchoolClassResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sectionId(entity.getSectionId())
                .section(entity.getSection())
                .status(entity.getStatus())
                .label(lbl)
                .build();
    }
}
