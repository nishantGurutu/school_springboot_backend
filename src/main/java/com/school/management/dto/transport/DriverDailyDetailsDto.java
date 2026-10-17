package com.school.management.dto.transport;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDailyDetailsDto {
    private DriverDto driver;
    private TripDtos.TripSummaryDto todayTrip;
    @Builder.Default
    private List<TripDtos.TripSummaryDto> recentTrips = new ArrayList<>();
    private Boolean todayChecklistCompleted;
    private String todayChecklistSummary;
    private String todayStartLocation;
    private String todayStartTime;
}
