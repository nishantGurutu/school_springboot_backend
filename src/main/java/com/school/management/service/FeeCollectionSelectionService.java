package com.school.management.service;

import com.school.management.dto.FeeCollectionRequest;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class FeeCollectionSelectionService {
    private final SectionService sections;
    private final FeesTypeRepository types;
    private final FeesDiscountRepository discounts;
    private final FeePaymentMethodRepository payments;
    private final StudentRepository students;

    public void apply(FeeCollectionEntity entity, FeeCollectionRequest request) {
        // Existing clients can still submit legacy collections without catalog selections.
        if (request.getFeeTypeId() == null) {
            if (request.getSection() != null && !request.getSection().isBlank())
                throw new BadRequestException("Select a fee type");
            return;
        }
        String className = required(request.getClassName(), "Select a class");
        String section = required(request.getSection(), "Select a section");
        if (sections.getForClass(className).stream().noneMatch(s -> s.getName().equalsIgnoreCase(section)))
            throw new BadRequestException("Section does not belong to the selected active class");
        var type = types.findById(request.getFeeTypeId()).filter(t -> "Active".equalsIgnoreCase(t.getStatus()))
                .orElseThrow(() -> new BadRequestException("Select an active fee type"));
        String payment = required(request.getPaymentType(), "Select a payment type");
        if (!payments.existsByNameAndActiveTrue(payment)) throw new BadRequestException("Select an active payment type");
        String rollNo = required(request.getRollNo(), "Roll number is required");
        var matching = students.findByClassNameIgnoreCaseAndSectionIgnoreCase(className, section).stream()
                .filter(s -> rollNo.equals(s.getRollNo()))
                .filter(s -> s.getStatus() == com.school.management.domain.student.StudentStatus.ACTIVE).toList();
        if (matching.size() != 1) throw new BadRequestException("No unique active student found for this class, section and roll number");
        var student = matching.get(0);
        BigDecimal amount = money(request.getAmount(), "Enter a valid positive amount");
        if (amount.signum() <= 0) throw new BadRequestException("Amount must be greater than zero");
        BigDecimal reduction = BigDecimal.ZERO;
        entity.setDiscountId(null); entity.setDiscount(null);
        if (request.getDiscountId() != null) {
            var discount = discounts.findById(request.getDiscountId()).filter(d -> "Active".equalsIgnoreCase(d.getStatus()))
                    .orElseThrow(() -> new BadRequestException("Select an active discount"));
            String scope = discount.getFeesType();
            if (scope != null && !scope.isBlank() && !scope.equalsIgnoreCase("All")
                && java.util.Arrays.stream(scope.split(",")).noneMatch(n -> n.trim().equalsIgnoreCase(type.getName())))
                throw new BadRequestException("Discount is not applicable to the selected fee type");
            BigDecimal value = money(discount.getDiscountValue() == null ? null : discount.getDiscountValue().replace("%", ""), "Discount value is invalid");
            if (value.signum() < 0) throw new BadRequestException("Discount value is invalid");
            if ("Percentage".equalsIgnoreCase(discount.getDiscountType())) {
                if (value.compareTo(new BigDecimal("100")) > 0) throw new BadRequestException("Discount percentage cannot exceed 100");
                reduction = amount.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else if ("Fixed Amount".equalsIgnoreCase(discount.getDiscountType())) reduction = value;
            else throw new BadRequestException("Unsupported discount type");
            if (reduction.compareTo(amount) > 0) throw new BadRequestException("Discount cannot exceed the fee amount");
            entity.setDiscountId(discount.getId()); entity.setDiscount(discount.getName());
        }
        try { LocalDate.parse(request.getDate()); } catch (Exception e) { throw new BadRequestException("Select a valid collection date"); }
        entity.setClassName(student.getClassName()); entity.setSection(student.getSection());
        entity.setName(student.getName()); entity.setAdmissionNo(student.getAdmissionNo());
        entity.setRollNo(student.getRollNo()); entity.setAvatar(student.getStudentPhoto());
        entity.setFeeTypeId(type.getId()); entity.setFeeType(type.getName());
        entity.setDiscountAmount(reduction.setScale(2, RoundingMode.HALF_UP).toPlainString());
        entity.setAmount(amount.toPlainString()); entity.setPaid(amount.subtract(reduction).toPlainString());
        entity.setDue("0.00"); entity.setStatus("Paid"); entity.setPaymentType(payment);
    }
    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new BadRequestException(message);
        return value.trim();
    }
    private BigDecimal money(String value, String message) {
        try { return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP); }
        catch (Exception e) { throw new BadRequestException(message); }
    }
}
