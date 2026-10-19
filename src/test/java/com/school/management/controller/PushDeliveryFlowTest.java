package com.school.management.controller;
import com.school.management.service.*;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import com.school.management.domain.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional
class PushDeliveryFlowTest {
 @Autowired PushDeviceController controller;
 @Autowired PushDeviceRepository devices;
 @Autowired PushDeliveryRepository deliveries;
 @Autowired PortalNotificationService notifications;
 @Autowired PushDeliveryService worker;
 @MockBean SecurityUtil security;
 @MockBean FirebasePushGateway gateway;
 void login(Long id){when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().id(id).role(Role.PARENT).email("push"+id+"@test.local").build()));}
 @Test void deliveryIsQueuedThenSentOnceWithoutBlockingBusinessWrite() throws Exception {
  login(101L);String token="test-registration-token-101";controller.register(new PushDeviceController.Registration(token));
  notifications.send(101L,"Homework assigned","Fractions","homework","1");
  var delivery=deliveries.findAll().get(0);assertNull(delivery.getDeliveredAt());
  when(gateway.send(eq(token),anyString(),anyString(),anyString())).thenReturn(200);
  worker.deliver(delivery.getId());worker.deliver(delivery.getId());
  assertNotNull(deliveries.findById(delivery.getId()).orElseThrow().getDeliveredAt());
  verify(gateway,times(1)).send(eq(token),anyString(),anyString(),anyString());
 }
 @Test void accountSwitchNeverDeliversOldUsersNotificationToReassignedDevice() throws Exception {
  String token="test-registration-token-switch";login(102L);controller.register(new PushDeviceController.Registration(token));
  notifications.send(102L,"Private message","For previous account","chat","1");
  var delivery=deliveries.findAll().get(0);
  login(103L);controller.register(new PushDeviceController.Registration(token));worker.deliver(delivery.getId());
  assertTrue(deliveries.findById(delivery.getId()).orElseThrow().isAbandoned());
  verify(gateway,never()).send(anyString(),anyString(),anyString(),anyString());
  controller.remove(new PushDeviceController.Registration(token));assertTrue(devices.findAll().isEmpty());
 }
}
