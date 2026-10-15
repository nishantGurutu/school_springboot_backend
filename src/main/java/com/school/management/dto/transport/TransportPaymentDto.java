package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportPaymentDto {
    private Long id;
    private String schoolId;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String receiptNumber;
    private Double amountPaid;
    private LocalDate paymentDate;
    private String paymentMode; // CASH, UPI, CARD, BANK_TRANSFER
    private String feeMonth;
    private String remarks;
    private String receivedBy;
}
