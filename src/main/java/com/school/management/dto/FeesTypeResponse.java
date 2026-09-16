package com.school.management.dto;

import com.school.management.entity.FeesTypeEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeesTypeResponse {

    private Long id;
    private String name;
    private String status;

    public static FeesTypeResponse fromEntity(FeesTypeEntity entity) {
        return FeesTypeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .build();
    }
}