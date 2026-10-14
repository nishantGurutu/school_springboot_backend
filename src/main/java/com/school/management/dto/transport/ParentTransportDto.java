package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentTransportDto {
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String className;
    private Long routeId;
    private String routeName;
    private String stopName;
    private String scheduledPickupTime;
    private String scheduledDropTime;
    private String pickupStatus; // SCHEDULED, PICKED_UP, DROPPED, ABSENT
    private LocalDateTime statusUpdatedAt;
    private String tripStatus; // SCHEDULED, IN_PROGRESS, COMPLETED
    private Long busId;
    private String busNumber;
    private String driverName;
    private String driverPhone;
    private Double busLatitude;
    private Double busLongitude;
    private Double busSpeedKmh;
    private Integer estimatedArrivalMinutes;
    private String currentStopName;
    private LocalDateTime lastGpsUpdate;
}
