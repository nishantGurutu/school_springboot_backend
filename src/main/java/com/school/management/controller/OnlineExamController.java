package com.school.management.controller;
import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/mobile/online-exams") @RequiredArgsConstructor @Transactional
public class OnlineExamController {
 private final PortalAccessService access;
 private final OnlineExamRepository exams;
 private final OnlineExamAttemptRepository attempts;
 private final PortalNotificationService notifications;
 private final ObjectMapper json;
 private final StudentRepository students;
 public record Question(String prompt,List<String> options,Integer correctIndex,Integer marks) {}
 public record Definition(String title,String subject,String className,String section,Integer durationMinutes,Instant opensAt,Instant closesAt,boolean published,List<Question> questions) {}
 public record Answers(Map<String,Integer> answers) {}
 private String encode(Object value) { try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException(e);} }
 private List<Question> questions(String value) { try{return json.readValue(value,new TypeReference<List<Question>>(){});}catch(Exception e){throw new IllegalStateException(e);} }
 private Map<String,Integer> answers(String value) { try{return value==null?Map.of():json.readValue(value,new TypeReference<Map<String,Integer>>(){});}catch(Exception e){throw new IllegalStateException(e);} }
 private boolean assigned(OnlineExamEntity e,StudentEntity s) { return e.getClassName().equalsIgnoreCase(s.getClassName()) && (e.getSection()==null || e.getSection().isBlank() || e.getSection().equalsIgnoreCase(s.getSection())); }
 private OnlineExamEntity managed(Long id) { OnlineExamEntity e=exams.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Exam not found")); access.requireClass(e.getClassName(),e.getSection()); if(access.actor().getRole()==Role.TEACHER && !Objects.equals(access.actor().getId(),e.getAuthorUserId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Exam belongs to another teacher"); return e; }
 private void finish(OnlineExamAttemptEntity a) {
  if(a.getSubmittedAt()!=null) return;
  List<Question> qs=questions(a.getQuestionsJson()); Map<String,Integer> selections=answers(a.getAnswersJson()); int score=0,max=0;
  for(int i=0;i<qs.size();i++){ Question q=qs.get(i); max+=q.marks(); if(Objects.equals(selections.get(Integer.toString(i)),q.correctIndex()))score+=q.marks(); }
  a.setScore(score);a.setMaximum(max);a.setSubmittedAt(Instant.now());attempts.save(a);
 }
 private Map<String,Object> view(OnlineExamAttemptEntity a) {
  if(a.getSubmittedAt()==null && !Instant.now().isBefore(a.getDeadline()))finish(a);
  Map<String,Object> result=new HashMap<>();result.put("id",a.getId());result.put("examId",a.getExamId());result.put("studentId",a.getStudentId());result.put("deadline",a.getDeadline());result.put("serverTime",Instant.now());result.put("submittedAt",a.getSubmittedAt());result.put("score",a.getScore());result.put("maximum",a.getMaximum());result.put("answers",answers(a.getAnswersJson()));
  students.findById(a.getStudentId()).ifPresent(student -> result.put("studentName",student.getName()));
  if(a.getSubmittedAt()==null)result.put("questions",questions(a.getQuestionsJson()).stream().map(q -> Map.of("prompt",q.prompt(),"options",q.options(),"marks",q.marks())).toList());
  return result;
 }
 @PostMapping public Map<String,Object> create(@RequestBody Definition d) {
  access.requireClass(d.className(),d.section());
  if(d.title()==null || d.title().isBlank() || d.subject()==null || d.subject().isBlank() || d.durationMinutes()==null || d.durationMinutes()<1 || d.durationMinutes()>300 || d.opensAt()==null || d.closesAt()==null || !d.closesAt().isAfter(d.opensAt()) || !d.closesAt().isAfter(Instant.now()) || d.questions()==null || d.questions().isEmpty() || d.questions().size()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide title, subject, duration, availability and questions");
  for(Question q:d.questions())if(q==null || q.prompt()==null || q.prompt().isBlank() || q.options()==null || q.options().size()<2 || q.options().size()>8 || q.options().stream().anyMatch(o -> o==null || o.isBlank()) || q.correctIndex()==null || q.correctIndex()<0 || q.correctIndex()>=q.options().size() || q.marks()==null || q.marks()<1 || q.marks()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid question options, answer or marks");
  OnlineExamEntity e=exams.save(OnlineExamEntity.builder().title(d.title().trim()).subject(d.subject().trim()).className(d.className()).section(d.section()).durationMinutes(d.durationMinutes()).opensAt(d.opensAt()).closesAt(d.closesAt()).published(d.published()).authorUserId(access.actor().getId()).questionsJson(encode(d.questions())).build());
  if(e.isPublished())access.eligible(e.getClassName(),e.getSection()).forEach(s -> notifications.family(s,"Online exam published",e.getTitle(),"onlineExams",e.getId().toString()));
  return Map.of("id",e.getId());
 }
 @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false) String admissionNo) {
  StudentEntity student=access.manager()?null:access.student(admissionNo);
  List<Map<String,Object>> output=new ArrayList<>();
  for(OnlineExamEntity e:exams.findAllByOrderByIdDesc()) {
   if(student!=null && (!e.isPublished() || !assigned(e,student)))continue;
   if(student==null && !access.teaches(e.getClassName(),e.getSection()))continue;
   Map<String,Object> row=new HashMap<>();row.put("id",e.getId());row.put("title",e.getTitle());row.put("subject",e.getSubject());row.put("className",e.getClassName());row.put("section",e.getSection());row.put("durationMinutes",e.getDurationMinutes());row.put("opensAt",e.getOpensAt());row.put("closesAt",e.getClosesAt());row.put("published",e.isPublished());row.put("questionCount",questions(e.getQuestionsJson()).size());
   if(student!=null)attempts.findByExamIdAndStudentId(e.getId(),student.getId()).ifPresent(a -> {Map<String,Object> summary=new HashMap<>(view(a));summary.remove("questions");summary.remove("answers");row.put("attempt",summary);});
   output.add(row);
  }return output;
 }
 @GetMapping("/{id}/results") public List<Map<String,Object>> results(@PathVariable Long id){managed(id);return attempts.findByExamId(id).stream().map(this::view).toList();}
 @PostMapping("/{id}/start") public Map<String,Object> start(@PathVariable Long id) {
  if(access.actor().getRole()!=Role.STUDENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only students can attempt an exam");
  StudentEntity s=access.student(null);OnlineExamEntity e=exams.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Exam not found"));
  if(!e.isPublished() || !assigned(e,s))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Exam is not assigned to you");
  var existing=attempts.findByExamIdAndStudentId(id,s.getId());if(existing.isPresent())return view(existing.get());
  Instant now=Instant.now();if(now.isBefore(e.getOpensAt()) || !now.isBefore(e.getClosesAt()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Exam is outside its availability window");
  Instant deadline=now.plusSeconds(e.getDurationMinutes()*60L);if(deadline.isAfter(e.getClosesAt()))deadline=e.getClosesAt();
  return view(attempts.save(OnlineExamAttemptEntity.builder().examId(id).studentId(s.getId()).startedAt(now).deadline(deadline).questionsJson(e.getQuestionsJson()).answersJson("{}").build()));
 }
 private OnlineExamAttemptEntity own(Long id) { if(access.actor().getRole()!=Role.STUDENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only students can submit answers"); StudentEntity s=access.student(null); return attempts.lockById(id).filter(a -> a.getStudentId().equals(s.getId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Attempt not found")); }
 @PostMapping("/attempts/{id}/answers") public Map<String,Object> save(@PathVariable Long id,@RequestBody Answers body) {
  OnlineExamAttemptEntity a=own(id); if(a.getSubmittedAt()!=null || !Instant.now().isBefore(a.getDeadline())){finish(a);return view(a);}
  List<Question> qs=questions(a.getQuestionsJson());Map<String,Integer> merged=new HashMap<>(answers(a.getAnswersJson()));
  if(body.answers()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Answers are required");
  for(var entry:body.answers().entrySet()){int index;try{index=Integer.parseInt(entry.getKey());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid question");} if(index<0 || index>=qs.size() || entry.getValue()==null || entry.getValue()<0 || entry.getValue()>=qs.get(index).options().size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid answer"); merged.put(entry.getKey(),entry.getValue());}
  a.setAnswersJson(encode(merged));attempts.save(a);return view(a);
 }
 @GetMapping("/attempts/{id}") public Map<String,Object> resume(@PathVariable Long id){return view(own(id));}
 @PostMapping("/attempts/{id}/submit") public Map<String,Object> submit(@PathVariable Long id){OnlineExamAttemptEntity a=own(id);finish(a);return view(a);}
}
