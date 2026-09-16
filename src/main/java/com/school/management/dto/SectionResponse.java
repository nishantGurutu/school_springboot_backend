package com.school.management.dto;

import com.school.management.entity.SectionEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionResponse {

    private Long id;
    private String name;
    private String status;

    public static SectionResponse fromEntity(SectionEntity entity) {
        return SectionResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .build();
    }
}
