package com.school.management.config;
import com.school.management.service.*;
import com.school.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;
@Configuration @RequiredArgsConstructor
public class PushDeliverySchedule {
 private final PushDeliveryRepository deliveries;
 private final FirebasePushGateway gateway;
 private final PushDeliveryService service;
 @Scheduled(initialDelay=30000,fixedDelay=15000) public void dispatch(){
  if(!gateway.configured())return;
  for(var delivery:deliveries.findTop50ByDeliveredAtIsNullAndAbandonedFalseAndNextAttemptAtBeforeOrderByIdAsc(Instant.now()))try{service.deliver(delivery.getId());}catch(Exception ignored){}
 }
}
