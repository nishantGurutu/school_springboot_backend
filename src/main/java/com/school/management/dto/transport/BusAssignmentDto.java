package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusAssignmentDto {
    private Long id;
    private String schoolId;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private Long busId;
    private String busNumber;
    private String registrationNumber;
    private Long routeId;
    private String routeName;
    private String routeCode;
    private String shift;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private String status;
    private String notes;
}
