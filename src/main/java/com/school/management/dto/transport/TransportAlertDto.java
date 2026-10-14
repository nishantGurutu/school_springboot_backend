package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAlertDto {
    private Long id;
    private Long tripId;
    private Long busId;
    private String busNumber;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private String status;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String resolutionRemarks;
    private LocalDateTime createdAt;
}
