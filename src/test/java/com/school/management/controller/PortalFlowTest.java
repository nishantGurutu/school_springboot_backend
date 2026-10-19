package com.school.management.controller;
import com.school.management.domain.user.Role;
import com.school.management.domain.student.Gender;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
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
class PortalFlowTest {
 @Autowired SchoolPortalController portal;
 @Autowired OnlineExamController exams;
 @Autowired StudentRepository students;
 @Autowired SchoolClassRepository classes;
 @Autowired UserRepository users;
 @Autowired OnlineExamAttemptRepository attempts;
 @MockBean SecurityUtil security;
 private UserEntity login(Role role,String email) {
  var user=users.findByEmailIgnoreCase(email).orElseGet(()->users.save(UserEntity.builder().name(role.name()).email(email).password("test-only").role(role).build()));
  when(security.getCurrentUser()).thenReturn(Optional.of(user));return user;
 }
 private StudentEntity child(String admission,String email,String parent) {
  return students.save(StudentEntity.builder().admissionNo(admission).name("Portal child").firstName("Portal").lastName("Child").rollNo(admission).className("Portal 8").section("A").gender(Gender.MALE).dateOfBirth(LocalDate.of(2013,1,1)).phone(admission).email(email).guardianEmail(parent).build());
 }
 @Test void diaryCertificateAndInboxUseLinkedChildIdentity() {
  classes.save(SchoolClassEntity.builder().name("Portal 8").section("A").build());
  login(Role.STUDENT,"portal.student@test.local");
  var parent=login(Role.PARENT,"portal.parent@test.local");
  var student=child("PORTAL-1","portal.student@test.local",parent.getEmail());
  login(Role.ADMIN,"portal.admin@test.local");
  var entry=portal.publish(DiaryEntryEntity.builder().title("Bring workbook").content("Chapter review tomorrow").className("Portal 8").section("A").build());
  var certificate=portal.issue(Map.of("admissionNo",student.getAdmissionNo(),"certificateName","Participation"));
  assertEquals(student.getAdmissionNo(),certificate.getAdmissionNo());
  login(Role.PARENT,parent.getEmail());
  assertEquals(entry.getId(),portal.diary(student.getAdmissionNo()).get(0).getId());
  assertEquals(1,portal.certificates(student.getAdmissionNo()).size());
  assertEquals(2,portal.notifications().size());
  var notification=portal.notifications().get(0);portal.read(notification.getId());assertNotNull(portal.notifications().get(0).getReadAt());
  login(Role.PARENT,"unrelated.portal@test.local");
  assertThrows(ResponseStatusException.class,()->portal.diary(student.getAdmissionNo()));
  assertThrows(ResponseStatusException.class,()->portal.certificates(student.getAdmissionNo()));
  assertThrows(ResponseStatusException.class,()->portal.read(notification.getId()));
  assertThrows(ResponseStatusException.class,()->portal.publish(entry));
 }
 @Test void examAutosavesScoresAndEnforcesDeadlineAndParentReadOnlyAccess() {
  classes.save(SchoolClassEntity.builder().name("Portal 8").section("A").build());
  var child=child("PORTAL-EXAM","portal.exam.student@test.local","portal.exam.parent@test.local");
  login(Role.ADMIN,"portal.exam.admin@test.local");
  var definition=new OnlineExamController.Definition("Fractions","Math","Portal 8","A",10,Instant.now().minusSeconds(60),Instant.now().plusSeconds(3600),true,List.of(new OnlineExamController.Question("Half plus half?",List.of("One","Two"),0,5),new OnlineExamController.Question("Two halves?",List.of("One","Three"),0,3)));
  Long id=((Number)exams.create(definition).get("id")).longValue();
  login(Role.STUDENT,child.getEmail());
  var started=exams.start(id);Long attemptId=((Number)started.get("id")).longValue();
  assertFalse(started.toString().contains("correctIndex"));
  assertEquals(attemptId,exams.start(id).get("id"));
  exams.save(attemptId,new OnlineExamController.Answers(Map.of("0",0,"1",1)));
  assertEquals(0,((Map<?,?>)exams.resume(attemptId).get("answers")).get("0"));
  var attempt=attempts.findById(attemptId).orElseThrow();attempt.setDeadline(Instant.now().minusSeconds(1));attempts.saveAndFlush(attempt);
  var expired=exams.save(attemptId,new OnlineExamController.Answers(Map.of("1",0)));
  assertNotNull(expired.get("submittedAt"));assertEquals(5,expired.get("score"));assertEquals(8,expired.get("maximum"));
  assertEquals(5,exams.submit(attemptId).get("score"));
  assertFalse(expired.containsKey("questions"));
  login(Role.PARENT,child.getGuardianEmail());
  var list=exams.list(child.getAdmissionNo());assertEquals(5,((Map<?,?>)list.get(0).get("attempt")).get("score"));
  assertThrows(ResponseStatusException.class,()->exams.start(id));
  assertThrows(ResponseStatusException.class,()->exams.submit(attemptId));
  login(Role.STUDENT,"other.portal.student@test.local");child("PORTAL-OTHER","other.portal.student@test.local","other.portal.parent@test.local");
  assertThrows(ResponseStatusException.class,()->exams.resume(attemptId));
 }
}
