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
    private boolean assigned;
    private String assignmentStatus;
    private String tripType;
    private Double stopLatitude;
    private Double stopLongitude;
    private String dropStopName;
    private Double dropLatitude;
    private Double dropLongitude;
    private Long tripId;
    private boolean gpsFresh;
    private String rollNo;
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String className;
    private Long routeId;
    private String routeName;
    private String pickupStopName;
    private String pickupTime;
    private String dropTime;
    private Long busId;
    private Double busLatitude;
    private Double busLongitude;
    private Double busSpeedKmh;
    private java.time.LocalDateTime lastGpsUpdate;
    private String pickupStatus;
    private String busNumber;
    private String driverName;
    private String driverPhone;
    private String tripStatus;
    private Integer estimatedArrivalMinutes;
    @Builder.Default
    private List<RouteStopDto> routeStops = new ArrayList<>();
}
