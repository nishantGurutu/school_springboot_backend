package com.school.management.dto.transport;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportFeeSummaryDto {
    private Double totalAssessed;
    private Double totalCollected;
    private Double totalPending;
    private Double totalOverdue;
    private Long paidCount;
    private Long pendingCount;
    private Long overdueCount;
    @Builder.Default
    private List<StudentAssignmentDto> studentRecords = new ArrayList<>();
    @Builder.Default
    private List<TransportPaymentDto> recentPayments = new ArrayList<>();
}
