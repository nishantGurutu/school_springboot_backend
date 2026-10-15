package com.school.management.dto.transport;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAssignmentDto {
    private Long id;
    private String schoolId;
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String className;
    private Long routeId;
    private String routeName;
    private Long pickupStopId;
    private String pickupStopName;
    private Long dropStopId;
    private String dropStopName;
    private String pickupTime;
    private String dropTime;
    private String guardianPhone;
    private String tripType;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
    private Double monthlyFee;
    private String feeStatus;
    private Double paidAmount;
    private Double pendingAmount;
    private String status;
    private String pickupStatus;
}
