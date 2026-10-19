package com.school.management.controller;
import com.school.management.service.*;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import com.school.management.domain.user.Role;
import com.school.management.domain.student.Gender;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional
class FeePaymentFlowTest {
 @Autowired FeePaymentService service;
 @Autowired FeeCollectionRepository fees;
 @Autowired FeePaymentRepository payments;
 @Autowired StudentRepository students;
 @Autowired ObjectMapper json;
 @MockBean SecurityUtil security;
 @MockBean RazorpayGateway gateway;
 private FeeCollectionEntity setup() {
  students.save(StudentEntity.builder().admissionNo("PAY-S").name("Pay Student").rollNo("PAY-1").firstName("Pay").lastName("Student").gender(Gender.MALE).dateOfBirth(LocalDate.of(2014,1,1)).phone("10000009").email("pay.student@test.local").guardianEmail("pay.parent@test.local").className("8").section("A").build());
  when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().id(123L).role(Role.PARENT).email("pay.parent@test.local").name("Parent").build()));
  when(gateway.configured()).thenReturn(true);
  when(gateway.create(anyLong(),anyString())).thenAnswer(inv->json.valueToTree(Map.of("id","plink_Test123","short_url","https://rzp.io/test","amount",inv.getArgument(0,Long.class),"currency","INR")));
  return fees.saveAndFlush(FeeCollectionEntity.builder().admissionNo("PAY-S").name("Pay Student").amount("1000.00").paid("200.00").due("800.00").status("Unpaid").build());
 }
 private void providerPaid(Long id,long amount) {
  var payment=payments.findById(id).orElseThrow();
  when(gateway.fetch(payment.getProviderId())).thenReturn(json.valueToTree(Map.of("id",payment.getProviderId(),"reference_id",payment.getReference(),"amount",amount,"currency","INR","amount_paid",amount,"status","paid","payments",List.of(Map.of("status","captured","amount",amount)))));
 }
 @Test void verifiedCaptureUpdatesFeeExactlyOnceAndReturnsReceipt() {
  var fee=setup();var checkout=service.checkout(fee.getId());Long id=((Number)checkout.get("id")).longValue();
  assertEquals("800.00",checkout.get("amount"));assertEquals("PENDING",checkout.get("status"));
  assertEquals("200.00",fees.findById(fee.getId()).orElseThrow().getPaid());
  providerPaid(id,80000);
  assertEquals("PAID",service.verify(id).get("status"));
  assertNotNull(service.verify(id).get("receipt"));
  assertEquals("1000.00",fees.findById(fee.getId()).orElseThrow().getPaid());
  assertEquals("0.00",fees.findById(fee.getId()).orElseThrow().getDue());
  verify(gateway,times(1)).fetch("plink_Test123");
 }
 @Test void providerAmountMismatchNeverCreditsFee() {
  var fee=setup();Long id=((Number)service.checkout(fee.getId()).get("id")).longValue();providerPaid(id,1);
  assertThrows(ResponseStatusException.class,()->service.verify(id));assertEquals("200.00",fee.getPaid());
 }
 @Test void changedInvoiceRequiresSchoolReconciliation() {
  var fee=setup();Long id=((Number)service.checkout(fee.getId()).get("id")).longValue();providerPaid(id,80000);
  fee.setPaid("300.00");fees.saveAndFlush(fee);
  assertEquals("REVIEW_REQUIRED",service.verify(id).get("status"));assertEquals("300.00",fee.getPaid());
 }
 @Test void unrelatedParentCannotCreateCheckoutOrVerifyAnotherChildPayment() {
  var fee=setup();Long id=((Number)service.checkout(fee.getId()).get("id")).longValue();
  when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().id(124L).role(Role.PARENT).email("unrelated.pay@test.local").build()));
  assertThrows(ResponseStatusException.class,()->service.checkout(fee.getId()));
  assertThrows(ResponseStatusException.class,()->service.verify(id));
  verify(gateway,never()).fetch(anyString());
 }
}
