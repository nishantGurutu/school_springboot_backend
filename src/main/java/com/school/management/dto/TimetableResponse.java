package com.school.management.dto;

import com.school.management.entity.TimetableEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TimetableResponse {

    private Long id;
    private String className;
    private String section;
    private String dayOfWeek;
    private String periodName;
    private String subject;
    private Long teacherId;
    private String teacherName;
    private String classroom;
    private String startTime;
    private String endTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TimetableResponse fromEntity(TimetableEntity entity) {
        if (entity == null) return null;
        return TimetableResponse.builder()
                .id(entity.getId())
                .className(entity.getClassName())
                .section(entity.getSection())
                .dayOfWeek(entity.getDayOfWeek())
                .periodName(entity.getPeriodName())
                .subject(entity.getSubject())
                .teacherId(entity.getTeacherId())
                .teacherName(entity.getTeacherName())
                .classroom(entity.getClassroom())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
