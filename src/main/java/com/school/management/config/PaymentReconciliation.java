package com.school.management.config;
import com.school.management.service.*;
import com.school.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import lombok.extern.slf4j.Slf4j;
@Configuration @EnableScheduling @RequiredArgsConstructor @Slf4j
public class PaymentReconciliation {
 private final FeePaymentRepository payments;
 private final FeePaymentService service;
 private final RazorpayGateway gateway;
 @Scheduled(initialDelayString="${school.payments.reconcile-delay-ms:60000}",fixedDelayString="${school.payments.reconcile-delay-ms:60000}") public void reconcile(){
  if(!gateway.configured())return;
  for(var payment:payments.findTop50ByStatusOrderByIdAsc("PENDING"))try{service.reconcilePayment(payment.getId());}catch(Exception e){log.warn("Payment {} verification deferred",payment.getId());}
 }
}
