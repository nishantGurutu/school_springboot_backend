package com.school.management.dto.transport;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLicenseDto {
    private String licenseNumber;
    private String licenseExpiryDate;
    private String licenseCategory;
    private String verificationStatus;
    private Boolean isVerified;
}
