package com.school.management.dto;

import com.school.management.entity.ExamResultEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExamResultResponse {

    private Long id;
    private String admissionNo;
    private String name;
    private String rollNo;
    private String className;
    private String exam;
    private Integer total;
    private Integer percent;
    private String grade;
    private String result;
    private String avatar;

    public static ExamResultResponse fromEntity(ExamResultEntity entity) {
        return ExamResultResponse.builder()
                .id(entity.getId())
                .admissionNo(entity.getAdmissionNo())
                .name(entity.getName())
                .rollNo(entity.getRollNo())
                .className(entity.getClassName())
                .exam(entity.getExam())
                .total(entity.getTotal())
                .percent(entity.getPercent())
                .grade(entity.getGrade())
                .result(entity.getResult())
                .avatar(entity.getAvatar())
                .build();
    }
}