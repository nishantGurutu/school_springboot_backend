package com.school.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.school.management.domain.user.Role;
import com.school.management.entity.FeeCollectionEntity;
import com.school.management.entity.FeePaymentEntity;
import com.school.management.entity.StudentEntity;
import com.school.management.repository.FeeCollectionRepository;
import com.school.management.repository.FeePaymentRepository;
import com.school.management.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class FeePaymentService {
 private final jakarta.persistence.EntityManager entityManager;
 private final FeeCollectionRepository fees;
 private final FeePaymentRepository payments;
 private final PortalAccessService access;
 private final RazorpayGateway gateway;
 private final PortalNotificationService notifications;
 private long minor(String value){try{return new BigDecimal(value==null || value.isBlank()?"0":value.replace(",","")).movePointRight(2).longValueExact();}catch(Exception e){throw new ResponseStatusException(HttpStatus.CONFLICT,"School must correct the fee amount before online payment");}}
 private String money(long amount){return BigDecimal.valueOf(amount,2).toPlainString();}
 private FeeCollectionEntity locked(Long id){return fees.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Fee not found"));}
 private void authorize(FeeCollectionEntity fee){
  if(!Set.of(Role.STUDENT,Role.PARENT).contains(access.actor().getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Student or parent payment access required");
  access.student(fee.getAdmissionNo());
 }
 public Map<String,Object> checkout(Long feeId){
  var fee=locked(feeId);authorize(fee);
  if(!gateway.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Online payments need school Razorpay configuration");
  for(var old:payments.findByFeeIdOrderByIdDesc(feeId)){
   if("PENDING".equals(old.getStatus())){
    reconcile(fee,old);
    if("PENDING".equals(old.getStatus()))return view(old);
    if("PAID".equals(old.getStatus()) || "REVIEW_REQUIRED".equals(old.getStatus()))return view(old);
   }
   if("REVIEW_REQUIRED".equals(old.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"School must reconcile the previous payment");
  }
  long total=minor(fee.getAmount()),paid=minor(fee.getPaid()),due=total-paid;
  if(due<=0 || paid<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"No valid outstanding fee amount");
  String reference=UUID.randomUUID().toString().replace("-","");
  JsonNode link=gateway.create(due,reference);
  String providerId=link.path("id").asText(),url=link.path("short_url").asText();
  if(!providerId.matches("plink_[A-Za-z0-9]+") || !url.startsWith("https://") || link.path("amount").asLong(-1)!=due || !"INR".equals(link.path("currency").asText()))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid checkout response from payment provider");
  var payment=payments.save(FeePaymentEntity.builder().feeId(feeId).providerId(providerId).reference(reference).checkoutUrl(url).amountMinor(due).paidBeforeMinor(paid).status("PENDING").createdAt(Instant.now()).build());
  return view(payment);
 }
 public Map<String,Object> verify(Long id){
  var payment=payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payment not found"));
  var fee=locked(payment.getFeeId());authorize(fee);
  // Reload after locking the fee so concurrent polls observe committed status.
  payment=payments.findById(id).orElseThrow();entityManager.refresh(payment);reconcile(fee,payment);return view(payment);
 }
 public void reconcilePayment(Long id){var payment=payments.findById(id).orElseThrow();var fee=locked(payment.getFeeId());payment=payments.findById(id).orElseThrow();entityManager.refresh(payment);reconcile(fee,payment);}
 private void reconcile(FeeCollectionEntity fee,FeePaymentEntity payment){
  if(!"PENDING".equals(payment.getStatus()))return;
  JsonNode remote=gateway.fetch(payment.getProviderId());
  if(!payment.getProviderId().equals(remote.path("id").asText()) || !payment.getReference().equals(remote.path("reference_id").asText()) || remote.path("amount").asLong(-1)!=payment.getAmountMinor() || !"INR".equals(remote.path("currency").asText()))throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment verification did not match the school invoice");
  String status=remote.path("status").asText();
  if(Set.of("expired","cancelled").contains(status)){payment.setStatus(status.toUpperCase());payments.save(payment);return;}
  if(!"paid".equals(status))return;
  long captured=0;for(JsonNode item:remote.path("payments"))if("captured".equals(item.path("status").asText()))captured+=item.path("amount").asLong();
  if(remote.path("amount_paid").asLong(-1)!=payment.getAmountMinor() || captured!=payment.getAmountMinor())throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment has not been fully captured");
  payment.setVerifiedAt(Instant.now());
  if(minor(fee.getPaid())!=payment.getPaidBeforeMinor() || minor(fee.getAmount())!=payment.getPaidBeforeMinor()+payment.getAmountMinor()){
   payment.setStatus("REVIEW_REQUIRED");payments.save(payment);return;
  }
  long paid=payment.getPaidBeforeMinor()+payment.getAmountMinor();fee.setPaid(money(paid));fee.setDue("0.00");fee.setStatus("Paid");fee.setPaymentType("Razorpay");fee.setDate(LocalDate.now().toString());fees.save(fee);
  payment.setStatus("PAID");payment.setReceipt("RZP-"+payment.getProviderId());fee.setOnlineReceipt(payment.getReceipt());fees.save(fee);payments.save(payment);
  var student=accessStudent(fee.getAdmissionNo());student.ifPresent(s->notifications.family(s,"Fee payment verified",payment.getReceipt(),"fees",fee.getId().toString()));
 }
 private final StudentRepository students;
 private Optional<StudentEntity> accessStudent(String admission){return students.findByAdmissionNoIgnoreCase(admission);}
 private Map<String,Object> view(FeePaymentEntity p){var row=new HashMap<String,Object>();row.put("id",p.getId());row.put("feeId",p.getFeeId());row.put("status",p.getStatus());row.put("checkoutUrl",p.getCheckoutUrl());row.put("amount",money(p.getAmountMinor()));row.put("receipt",p.getReceipt());row.put("verifiedAt",p.getVerifiedAt());return row;}
}
