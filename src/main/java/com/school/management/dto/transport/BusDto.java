package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusDto {
    private Long id;
    private String schoolId;
    private String busNumber;
    private String registrationNumber;
    private Integer capacity;
    private String model;
    private String manufacturer;
    private String fuelType;
    private String gpsDeviceId;
    private LocalDate insuranceExpiryDate;
    private LocalDate fitnessExpiryDate;
    private LocalDate permitExpiryDate;
    private LocalDate pollutionExpiryDate;
    private String status;
    private String assignmentStatus; // AVAILABLE or ASSIGNED, independent of operational status
    private Boolean availableForAssignment;
    private Double currentSpeed;
    private Double lastLatitude;
    private Double lastLongitude;
    private LocalDateTime lastGpsUpdate;
    private Long assignedDriverId;
    private String assignedDriverName;
    private Long assignedRouteId;
    private String assignedRouteName;
    private String activeTripStatus;
    private Long activeTripId;
}
