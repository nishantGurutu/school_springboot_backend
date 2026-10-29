package com.school.management.chat;

import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @Transactional
class SchoolChatFlowTest {
 @Autowired SchoolChatService chat;
 @Autowired ChatPolicy policy;
 @Autowired UserRepository users;
 @Autowired ChatMemberRepository members;
 @Autowired SchoolMessageRepository messages;
 @Autowired ChatRoomRepository rooms;
 @Autowired PortalNotificationRepository notifications;
 @Autowired SchoolChatController controller;
 @MockBean SecurityUtil security;
 @MockBean ChatFileStorage storage;
 UserEntity user(String name,Role role,String school){return users.save(UserEntity.builder().name(name).email(name+".chat@test.local").password("unused").role(role).schoolId(school).build());}
 void as(UserEntity u){when(security.getCurrentUser()).thenReturn(Optional.of(u));}
 String direct(UserEntity a,UserEntity b){as(a);return chat.create(new SchoolChatService.Create("",false,List.of(b.getId()))).get("id").toString();}
 @SuppressWarnings("unchecked") List<Map<String,Object>> history(String room,long after){return (List<Map<String,Object>>)chat.history(room,after).get("messages");}
 @Test void directMessageIsIdempotentPersistsAndUpdatesUnreadDeliveredRead(){
  var admin=user("sender",Role.ADMIN,"A");var staff=user("recipient",Role.STAFF,"A");String room=direct(admin,staff);
  var sent=chat.send(room,new SchoolChatService.Send("same-client-id","Hello",null));
  var again=chat.send(room,new SchoolChatService.Send("same-client-id","Hello",null));assertEquals(sent.get("id"),again.get("id"));assertEquals(1,messages.count());
  assertEquals(1,notifications.findByUserIdOrderByIdDesc(staff.getId()).size());
  long id=((Number)sent.get("id")).longValue();as(staff);assertEquals(1L,chat.list().get(0).get("unreadCount"));
  chat.receipt(room,id,false);as(admin);assertEquals("delivered",history(room,0).get(0).get("status"));
  as(staff);chat.receipt(room,id,true);assertEquals(0L,chat.list().get(0).get("unreadCount"));
  as(admin);assertEquals("read",history(room,0).get(0).get("status"));assertTrue(history(room,id).isEmpty());
 }
 @Test void directRoomIsReusedAndTenantAndMembershipAreEnforced(){
  var admin=user("leader",Role.ADMIN,"A");var staff=user("worker",Role.STAFF,"A");var other=user("other",Role.ADMIN,"B");var outsider=user("outsider",Role.STAFF,"A");
  String room=direct(admin,staff);assertEquals(room,direct(staff,admin));assertEquals(1,rooms.count());
  as(other);assertFalse(policy.contacts().stream().anyMatch(c->c.get("id").equals(admin.getId())));
  assertEquals(403,assertThrows(ResponseStatusException.class,()->chat.history(room,0)).getStatusCode().value());
  assertThrows(ResponseStatusException.class,()->chat.create(new SchoolChatService.Create("",false,List.of(admin.getId()))));
  as(outsider);assertThrows(ResponseStatusException.class,()->chat.send(room,new SchoolChatService.Send("attack","No",null)));
 }
 @Test void leadersCreateGroupsFamilyCannotAndForgedReceiptIsRejected(){
  var admin=user("groupadmin",Role.PRINCIPAL,"A");var student=user("groupstudent",Role.STUDENT,"A");var staff=user("groupstaff",Role.STAFF,"A");
  as(admin);String group=chat.create(new SchoolChatService.Create("School announcements",true,List.of(student.getId(),staff.getId()))).get("id").toString();
  long id=((Number)chat.send(group,new SchoolChatService.Send("g1","Notice",null)).get("id")).longValue();
  String second=direct(admin,staff);assertThrows(ResponseStatusException.class,()->chat.receipt(second,id,true));
  as(student);assertThrows(ResponseStatusException.class,()->chat.create(new SchoolChatService.Create("Forged",true,List.of(admin.getId()))));
  assertEquals(1,history(group,0).size());
 }
 @Test void parentStudentContactRequiresActualFamilyLink(){
  var parent=user("parent",Role.PARENT,"A");var own=user("ownchild",Role.STUDENT,"A");var other=user("unrelated",Role.STUDENT,"A");
  var child=StudentEntity.builder().admissionNo("CHAT-CHILD-1").name("Linked Child").firstName("Linked").lastName("Child").email(own.getEmail()).guardianEmail(parent.getEmail()).rollNo("1").className("Chat Class").section("A").phone("1234567890").gender(com.school.management.domain.student.Gender.MALE).dateOfBirth(java.time.LocalDate.of(2014,1,1)).build();
  students.save(child);assertTrue(policy.contact(parent,own));assertFalse(policy.contact(parent,other));
 }
 @Autowired StudentRepository students;
 @Test void attachmentsArePrivateValidatedAndBoundToRoomAndSender() throws Exception {
  var admin=user("fileadmin",Role.ADMIN,"A");var staff=user("filestaff",Role.STAFF,"A");var stranger=user("filestranger",Role.ADMIN,"A");String room=direct(admin,staff);
  when(storage.put(anyString(),any(),anyString())).thenReturn("local:test");
  var pdf=new MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.4 test document".getBytes());
  @SuppressWarnings("unchecked") var file=(Map<String,Object>)controller.upload(room,pdf);String attachment=file.get("id").toString();
  assertThrows(ResponseStatusException.class,()->controller.upload(room,new MockMultipartFile("file","evil.pdf","application/pdf","<script>evil</script>".getBytes())));
  as(staff);assertThrows(ResponseStatusException.class,()->chat.send(room,new SchoolChatService.Send("stolen","",attachment)));
  as(stranger);assertThrows(ResponseStatusException.class,()->controller.download(attachment));
  as(admin);assertNotNull(chat.send(room,new SchoolChatService.Send("real","",attachment)).get("attachment"));
 }
 @Test void reconnectCursorPagesHistoryWithoutLosingOrRepeatingMessages(){
  var admin=user("pageadmin",Role.ADMIN,"A");var staff=user("pagestaff",Role.STAFF,"A");String room=direct(admin,staff);
  for(int i=0;i<105;i++)chat.send(room,new SchoolChatService.Send("msg-"+i,"Message "+i,null));
  var first=history(room,0);assertEquals(100,first.size());long cursor=((Number)first.get(99).get("id")).longValue();var next=history(room,cursor);assertEquals(5,next.size());assertEquals("Message 100",next.get(0).get("text"));
 }
 @Autowired TeacherRepository teachers;
 @Test void teacherMayContactOnlyAssignedClassesAndClassRevocationBlocksDirectHistory(){
  var teacher=user("assignedteacher",Role.TEACHER,"A");var student=user("assignedstudent",Role.STUDENT,"A");
  students.save(StudentEntity.builder().admissionNo("CHAT-ASSIGNED-1").name("Assigned Student").firstName("Assigned").lastName("Student").email(student.getEmail()).rollNo("2").className("Grade 7").section("B").phone("1234567890").gender(com.school.management.domain.student.Gender.MALE).dateOfBirth(java.time.LocalDate.of(2014,1,1)).build());
  var profile=teachers.save(TeacherEntity.builder().employeeId("CHAT-T-1").firstName("Teacher").lastName("Test").email(teacher.getEmail()).department("Science").subject("Science").phone("1234567890").joiningDate(java.time.LocalDate.now()).assignedClass("Grade 7 - B").build());
  assertTrue(policy.contact(teacher,student));String room=direct(teacher,student);chat.send(room,new SchoolChatService.Send("class-linked","Hello class",null));
  profile.setAssignedClass("Grade 8 - A");teachers.saveAndFlush(profile);assertFalse(policy.contact(teacher,student));
  assertThrows(ResponseStatusException.class,()->chat.history(room,0));
 }

 @Test void videoAndDocumentsCanBeSharedAndDisguisedVideoIsRejected() throws Exception {
  var admin=user("videoadmin",Role.ADMIN,"A");var staff=user("videostaff",Role.STAFF,"A");String room=direct(admin,staff);
  when(storage.put(anyString(),any(),anyString())).thenReturn("local:video");
  byte[] mp4=new byte[]{0,0,0,24,'f','t','y','p','i','s','o','m',0,0,0,0};
  @SuppressWarnings("unchecked") var video=(Map<String,Object>)controller.upload(room,new MockMultipartFile("file","lesson.mp4","video/mp4",mp4));
  assertEquals("video/mp4",video.get("type"));chat.send(room,new SchoolChatService.Send("video-client","Lesson video",video.get("id").toString()));
  assertThrows(ResponseStatusException.class,()->controller.upload(room,new MockMultipartFile("file","fake.mp4","video/mp4","not a video".getBytes())));
  @SuppressWarnings("unchecked") var document=(Map<String,Object>)controller.upload(room,new MockMultipartFile("file","homework.txt","text/plain","Read chapter 2".getBytes()));
  assertEquals("text/plain",document.get("type"));
 }

}
