package com.school.management.dto;

import com.school.management.entity.SubjectEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectResponse {

    private Long id;
    private String name;
    private String code;
    private String status;

    private String className;

    public static SubjectResponse fromEntity(SubjectEntity entity) {
        return SubjectResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .status(entity.getStatus())
                .className(entity.getClassName())
                .build();
    }
}
