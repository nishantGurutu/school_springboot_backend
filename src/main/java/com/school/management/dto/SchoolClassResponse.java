package com.school.management.dto;

import com.school.management.entity.SchoolClassEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolClassResponse {

    private Long id;
    private String name;
    private String section;
    private String status;

    public static SchoolClassResponse fromEntity(SchoolClassEntity entity) {
        return SchoolClassResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .section(entity.getSection())
                .status(entity.getStatus())
                .build();
    }
}
