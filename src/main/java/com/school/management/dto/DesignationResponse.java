package com.school.management.dto;

import com.school.management.entity.DesignationEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DesignationResponse {

    private Long id;
    private String name;
    private String code;
    private String category;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DesignationResponse fromEntity(DesignationEntity entity) {
        return DesignationResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode() != null ? entity.getCode() : "")
                .category(entity.getCategory() != null ? entity.getCategory() : "Teaching")
                .description(entity.getDescription() != null ? entity.getDescription() : "")
                .status(entity.getStatus() != null ? entity.getStatus() : "Active")
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
