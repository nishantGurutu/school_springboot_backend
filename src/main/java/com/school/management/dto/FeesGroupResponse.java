package com.school.management.dto;

import com.school.management.entity.FeesGroupEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeesGroupResponse {

    private Long id;
    private String name;
    private String feesType;
    private String status;

    public static FeesGroupResponse fromEntity(FeesGroupEntity entity) {
        return FeesGroupResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .feesType(entity.getFeesType())
                .status(entity.getStatus())
                .build();
    }
}