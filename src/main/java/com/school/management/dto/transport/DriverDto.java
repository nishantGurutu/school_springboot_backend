package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDto {
    private Long id;
    private String schoolId;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String password; // optional on create/update
    private String licenseNumber;
    private LocalDate licenseExpiryDate;
    private LocalDate joiningDate;
    private String licenseCategory;
    private Integer experienceYears;
    private String verificationStatus;
    private String status;
    private String avatarUrl;
    private String emergencyContact;
    private String address;
    private Long assignedBusId;
    private String assignedBusNumber;
    private Long assignedRouteId;
    private String assignedRouteName;
    private String dutyStatus; // OFF_DUTY, SCHEDULED, IN_TRIP
}
