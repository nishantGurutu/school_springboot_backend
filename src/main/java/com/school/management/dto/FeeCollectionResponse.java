package com.school.management.dto;

import com.school.management.entity.FeeCollectionEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeeCollectionResponse {

    private Long id;
    private String admissionNo;
    private String name;
    private String rollNo;
    private String className;
    private String amount;
    private String paid;
    private String due;
    private String date;
    private String status;
    private String paymentType;
    private String note;
    private String avatar;

    public static FeeCollectionResponse fromEntity(FeeCollectionEntity entity) {
        return FeeCollectionResponse.builder()
                .id(entity.getId())
                .admissionNo(entity.getAdmissionNo())
                .name(entity.getName())
                .rollNo(entity.getRollNo())
                .className(entity.getClassName())
                .amount(entity.getAmount())
                .paid(entity.getPaid())
                .due(entity.getDue())
                .date(entity.getDate())
                .status(entity.getStatus())
                .paymentType(entity.getPaymentType())
                .note(entity.getNote())
                .avatar(entity.getAvatar())
                .build();
    }
}