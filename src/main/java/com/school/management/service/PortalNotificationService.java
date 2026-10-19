package com.school.management.service;
import com.school.management.entity.*;
import com.school.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service @RequiredArgsConstructor
public class PortalNotificationService {
 private final PortalNotificationRepository notifications;
 private final PushDeviceRepository devices;
 private final PushDeliveryRepository deliveries;
 private final UserRepository users;
 private final GuardianRepository guardians;
 @Transactional public void send(Long userId,String title,String body,String route,String reference) {
  if(userId==null) return;
  PortalNotificationEntity notification=notifications.save(PortalNotificationEntity.builder().userId(userId).title(title).body(body).route(route).referenceId(reference).createdAt(LocalDateTime.now()).build());
  devices.findByUserId(userId).forEach(device -> deliveries.save(PushDeliveryEntity.builder().deviceId(device.getId()).notificationId(notification.getId()).nextAttemptAt(java.time.Instant.now()).build()));
 }
 @Transactional public void family(StudentEntity student,String title,String body,String route,String reference) {
  Set<Long> ids=new HashSet<>();
  users.findByEmailIgnoreCase(Objects.toString(student.getEmail(),"")).ifPresent(u -> ids.add(u.getId()));
  users.findByEmailIgnoreCase(Objects.toString(student.getGuardianEmail(),"")).ifPresent(u -> ids.add(u.getId()));
  guardians.findAll().stream().filter(g -> g.getStudentAdmissionNo()!=null && Arrays.stream(g.getStudentAdmissionNo().split(",")).anyMatch(a -> a.trim().equalsIgnoreCase(student.getAdmissionNo())))
    .forEach(g -> users.findByEmailIgnoreCase(Objects.toString(g.getEmail(),"")).ifPresent(u -> ids.add(u.getId())));
  ids.forEach(id -> send(id,title,body,route,reference));
 }
}
