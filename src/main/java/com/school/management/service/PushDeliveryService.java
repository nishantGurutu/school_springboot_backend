package com.school.management.service;

import com.school.management.repository.PortalNotificationRepository;
import com.school.management.repository.PushDeliveryRepository;
import com.school.management.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service @RequiredArgsConstructor
public class PushDeliveryService {
 private final PushDeliveryRepository deliveries;
 private final PushDeviceRepository devices;
 private final PortalNotificationRepository notifications;
 private final FirebasePushGateway gateway;
 @Transactional public void deliver(Long id){
  var delivery=deliveries.lockById(id).orElseThrow();
  if(delivery.getDeliveredAt()!=null || delivery.isAbandoned())return;
  var device=devices.findById(delivery.getDeviceId()).orElse(null);
  var notification=notifications.findById(delivery.getNotificationId()).orElse(null);
  if(device==null || notification==null || !Objects.equals(device.getUserId(),notification.getUserId()) || notification.getReadAt()!=null){delivery.setAbandoned(true);deliveries.save(delivery);return;}
  int status=503;try{status=gateway.send(device.getToken(),notification.getTitle(),notification.getBody(),notification.getId().toString());}catch(InterruptedException e){Thread.currentThread().interrupt();}catch(Exception ignored){}
  delivery.setAttempts(delivery.getAttempts()+1);
  if(status>=200 && status<300)delivery.setDeliveredAt(Instant.now());
  else if(status==404){devices.delete(device);delivery.setAbandoned(true);}
  else if(delivery.getAttempts()>=6)delivery.setAbandoned(true);
  delivery.setNextAttemptAt(Instant.now().plusSeconds(Math.min(3600,30L*(1L<<delivery.getAttempts()))));deliveries.save(delivery);
 }
}
