package com.school.management.dto.transport;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentTransportDto {
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String className;
    private Long routeId;
    private String routeName;
    private String pickupStopName;
    private String pickupTime;
    private String dropStopName;
    private String dropTime;
    private String busNumber;
    private String driverName;
    private String driverPhone;
    private String tripStatus;
    private Integer estimatedArrivalMinutes;
    @Builder.Default
    private List<RouteStopDto> routeStops = new ArrayList<>();
}
