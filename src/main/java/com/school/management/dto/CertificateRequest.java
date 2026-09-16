package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertificateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String rollNo;

    private String className;

    private String certificateName;

    private String bgImage;

    private String avatar;

    private String date;

    private String footerLeft;

    private String footerRight;
}
