package com.school.management.dto.transport;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportOverviewDto {
    private long totalBuses;
    private long activeTrips;
    private long idleBuses;
    private long delayedTrips;
    private long offlineGpsCount;
    private long openAlertsCount;
    private long totalRoutes;
    private long totalDrivers;
    private long totalStudentsAssigned;
    @Builder.Default
    private List<TripDtos.LiveTrackingBusDto> liveBuses = new ArrayList<>();
    @Builder.Default
    private List<TransportAlertDto> recentAlerts = new ArrayList<>();
}
