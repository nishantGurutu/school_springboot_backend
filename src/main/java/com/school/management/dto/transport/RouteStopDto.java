package com.school.management.dto.transport;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopDto {
    private Long id;
    private Long routeId;
    private String stopName;
    private Integer stopOrder;
    private Double latitude;
    private Double longitude;
    private String plannedMorningPickupTime;
    private String plannedEveningDropTime;
    private Integer geofenceRadiusMeters;
    private String landmark;
    private Integer assignedStudentsCount;
    private String status; // PENDING, ARRIVED, DEPARTED
    private String actualArrivalTime;
    private Long dwellDurationSeconds;
}
