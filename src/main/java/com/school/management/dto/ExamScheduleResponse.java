package com.school.management.dto;

import com.school.management.entity.ExamScheduleEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExamScheduleResponse {

    private Long id;
    private String className;
    private String subject;
    private String date;
    private String startTime;
    private String endTime;
    private String duration;
    private String room;

    public static ExamScheduleResponse fromEntity(ExamScheduleEntity entity) {
        return ExamScheduleResponse.builder()
                .id(entity.getId())
                .className(entity.getClassName())
                .subject(entity.getSubject())
                .date(entity.getDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .duration(entity.getDuration())
                .room(entity.getRoom())
                .build();
    }
}