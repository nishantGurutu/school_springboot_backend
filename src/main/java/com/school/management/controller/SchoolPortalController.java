package com.school.management.controller;

import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.service.PortalAccessService;
import com.school.management.service.PortalNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController @RequestMapping("/api/mobile/portal") @RequiredArgsConstructor @Transactional(readOnly=true)
public class SchoolPortalController {
 private final PortalAccessService access;
 private final DiaryEntryRepository diary;
 private final PortalNotificationRepository notifications;
 private final PortalNotificationService delivery;
 private final CertificateRepository certificates;
 private final ExamResultRepository results;
 private final AttendanceRepository attendance;
 private final StudentRepository students;
 @GetMapping("/context") public Map<String,Object> context() {
  List<Map<String,Object>> children=access.children().stream().map(s -> Map.<String,Object>of("id",s.getId(),"name",Objects.toString(s.getName(),""),"admissionNo",s.getAdmissionNo(),"className",Objects.toString(s.getClassName(),""),"section",Objects.toString(s.getSection(),""))).toList();
  return Map.of("manager",access.manager(),"children",children,"classes",access.manager()?access.classes():List.of());
 }
 private boolean assigned(DiaryEntryEntity e,StudentEntity s) { return e.getTargetStudentId()!=null?e.getTargetStudentId().equals(s.getId()):e.getClassName().equalsIgnoreCase(s.getClassName()) && (e.getSection()==null || e.getSection().isBlank() || e.getSection().equalsIgnoreCase(s.getSection())); }
 @GetMapping("/diary") public List<DiaryEntryEntity> diary(@RequestParam(required=false) String admissionNo) {
  if(access.manager()) return diary.findAllByOrderByIdDesc().stream().filter(e -> access.teaches(e.getClassName(),e.getSection())).toList();
  StudentEntity s=access.student(admissionNo); return diary.findAllByOrderByIdDesc().stream().filter(e -> assigned(e,s)).toList();
 }
 @PostMapping("/diary") @Transactional public DiaryEntryEntity publish(@RequestBody DiaryEntryEntity request) {
  access.requireClass(request.getClassName(),request.getSection());
  if(request.getTitle()==null || request.getTitle().isBlank() || request.getContent()==null || request.getContent().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Title and diary entry are required");
  List<StudentEntity> audience=access.eligible(request.getClassName(),request.getSection());
  if(request.getTargetStudentId()!=null) { audience=audience.stream().filter(s -> s.getId().equals(request.getTargetStudentId())).toList(); if(audience.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Student is not in the selected class"); }
  request.setId(null); request.setAuthorUserId(access.actor().getId()); request.setPostedAt(LocalDateTime.now());
  DiaryEntryEntity saved=diary.save(request);
  audience.forEach(s -> delivery.family(s,"New diary entry",saved.getTitle(),"diary",saved.getId().toString())); return saved;
 }
 @DeleteMapping("/diary/{id}") @Transactional public void delete(@PathVariable Long id) {
  access.requireManager(); DiaryEntryEntity entry=diary.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Entry not found"));
  access.requireClass(entry.getClassName(),entry.getSection());
  if(access.actor().getRole()==com.school.management.domain.user.Role.TEACHER && !Objects.equals(access.actor().getId(),entry.getAuthorUserId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Entry belongs to another teacher");
  diary.delete(entry);
 }
 @GetMapping("/certificates") public List<CertificateEntity> certificates(@RequestParam(required=false) String admissionNo) {
  StudentEntity s=access.student(admissionNo); return certificates.findAll().stream().filter(c -> s.getAdmissionNo().equalsIgnoreCase(c.getAdmissionNo())).toList();
 }
 @PostMapping("/certificates") @Transactional public CertificateEntity issue(@RequestBody Map<String,String> request) {
  access.requireManager(); StudentEntity s=access.student(request.get("admissionNo"));
  String title=Objects.toString(request.get("certificateName"),"").trim(); if(title.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Certificate title is required");
  CertificateEntity c=certificates.save(CertificateEntity.builder().name(s.getName()).rollNo(s.getRollNo()).className(s.getClassName()).admissionNo(s.getAdmissionNo()).certificateName(title).date(LocalDate.now().toString()).footerLeft(request.get("footerLeft")).footerRight(request.get("footerRight")).build());
  delivery.family(s,"Certificate issued",title,"certificates",c.getId().toString()); return c;
 }
 @GetMapping("/report") public Map<String,Object> report(@RequestParam(required=false) String admissionNo) {
  StudentEntity s=access.student(admissionNo);
  List<AttendanceEntity> records=attendance.findAll().stream().filter(a -> s.getAdmissionNo().equalsIgnoreCase(a.getAdmissionNo()) && "STUDENT".equalsIgnoreCase(a.getAttendanceType())).toList();
  long present=records.stream().filter(a -> "present".equalsIgnoreCase(a.getStatus())).count();
  return Map.of("student",Map.of("name",Objects.toString(s.getName(),""),"admissionNo",s.getAdmissionNo(),"className",Objects.toString(s.getClassName(),""),"section",Objects.toString(s.getSection(),"")),"results",results.findByAdmissionNoIgnoreCaseOrderByIdDesc(s.getAdmissionNo()),"attendance",Map.of("total",records.size(),"present",present,"percentage",records.isEmpty()?0.0:100.0*present/records.size()));
 }
 @GetMapping("/notifications") public List<PortalNotificationEntity> notifications() { return notifications.findByUserIdOrderByIdDesc(access.actor().getId()); }
 @PostMapping("/notifications/{id}/read") @Transactional public void read(@PathVariable Long id) {
  PortalNotificationEntity n=notifications.findById(id).filter(row -> Objects.equals(row.getUserId(),access.actor().getId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));
  n.setReadAt(LocalDateTime.now()); notifications.save(n);
 }
}
