package com.school.management.entity.transport;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transport_fee_payments", indexes = {
        @Index(name = "idx_tf_school", columnList = "school_id"),
        @Index(name = "idx_tf_student", columnList = "student_id"),
        @Index(name = "idx_tf_assignment", columnList = "assignment_id"),
        @Index(name = "idx_tf_receipt", columnList = "receipt_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportFeePaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, length = 64)
    @Builder.Default
    private String schoolId = "SCH-001";

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 128)
    private String studentName;

    @Column(name = "admission_no", length = 32)
    private String admissionNo;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 64)
    private String receiptNumber;

    @Column(name = "amount_paid", nullable = false)
    private Double amountPaid;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_mode", length = 32)
    @Builder.Default
    private String paymentMode = "CASH"; // CASH, UPI, CARD, BANK_TRANSFER

    @Column(name = "fee_month", length = 32)
    private String feeMonth;

    @Column(length = 256)
    private String remarks;

    @Column(name = "received_by", length = 64)
    private String receivedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (schoolId == null || schoolId.isBlank()) {
            schoolId = "SCH-001";
        }
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
        if (receiptNumber == null || receiptNumber.isBlank()) {
            receiptNumber = "TR-REC-" + System.currentTimeMillis();
        }
    }
}
