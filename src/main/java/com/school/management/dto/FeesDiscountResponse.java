package com.school.management.dto;

import com.school.management.entity.FeesDiscountEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeesDiscountResponse {

    private Long id;
    private String name;
    private String feesType;
    private String discountType;
    private String discountValue;
    private String status;

    public static FeesDiscountResponse fromEntity(FeesDiscountEntity entity) {
        return FeesDiscountResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .feesType(entity.getFeesType())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .status(entity.getStatus())
                .build();
    }
}