package com.school.management.dto;

import com.school.management.entity.LeaveTypeEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveTypeResponse {

    private Long id;
    private String name;
    private String status;

    public static LeaveTypeResponse fromEntity(LeaveTypeEntity entity) {
        return LeaveTypeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .build();
    }
}
