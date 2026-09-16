package com.school.management.dto;

import com.school.management.entity.ExamEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExamResponse {

    private Long id;
    private String name;
    private String date;
    private String startTime;
    private String endTime;
    private String status;

    public static ExamResponse fromEntity(ExamEntity entity) {
        return ExamResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .date(entity.getDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .build();
    }
}