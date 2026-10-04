package com.school.management.dto;

import com.school.management.entity.DepartmentEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String headOfDepartment;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DepartmentResponse fromEntity(DepartmentEntity entity) {
        return DepartmentResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .headOfDepartment(entity.getHeadOfDepartment() != null ? entity.getHeadOfDepartment() : "")
                .description(entity.getDescription() != null ? entity.getDescription() : "")
                .status(entity.getStatus() != null ? entity.getStatus() : "Active")
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
