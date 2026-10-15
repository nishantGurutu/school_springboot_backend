package com.school.management.dto.transport;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteDto {
    private Long id;
    private String schoolId;
    private String routeName;
    private String routeCode;
    private String areaLocality;
    private String description;
    private String startLocation;
    private String endLocation;
    private Double fare;
    private Integer plannedDurationMinutes;
    private Double totalDistanceKm;
    private String morningStartTime;
    private String eveningStartTime;
    private Integer speedLimitKmh;
    private String status;
    private Integer stopsCount;
    private Integer assignedStudentsCount;
    @Builder.Default
    private List<RouteStopDto> stops = new ArrayList<>();
}
