package com.school.management.dto;

import com.school.management.entity.CertificateEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificateResponse {

    private Long id;
    private String name;
    private String rollNo;
    private String className;
    private String certificateName;
    private String bgImage;
    private String avatar;
    private String date;
    private String footerLeft;
    private String footerRight;

    public static CertificateResponse fromEntity(CertificateEntity entity) {
        return CertificateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .rollNo(entity.getRollNo())
                .className(entity.getClassName())
                .certificateName(entity.getCertificateName())
                .bgImage(entity.getBgImage())
                .avatar(entity.getAvatar())
                .date(entity.getDate())
                .footerLeft(entity.getFooterLeft())
                .footerRight(entity.getFooterRight())
                .build();
    }
}
