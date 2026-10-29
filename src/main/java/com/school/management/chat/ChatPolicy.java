package com.school.management.chat;
import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor
public class ChatPolicy {
 private final SecurityUtil security;
 private final UserRepository users;
 private final StudentRepository students;
 private final GuardianRepository guardians;
 private final TeacherRepository teachers;
 private static final Set<Role> LEADERS=Set.of(Role.ADMIN,Role.MASTER_ADMIN,Role.SUPER_ADMIN,Role.PRINCIPAL);
 private static final Set<Role> EMPLOYEES=Set.of(Role.TEACHER,Role.STAFF,Role.ACCOUNTANT,Role.LIBRARIAN,Role.DRIVER);
 public UserEntity actor(){var u=security.getCurrentUser().orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"Sign in required"));if(!u.isEnabled())throw error(HttpStatus.FORBIDDEN,"Account disabled");return u;}
 public boolean leader(UserEntity u){return LEADERS.contains(u.getRole());}
 public String school(UserEntity u){if(u.getSchoolId()==null || u.getSchoolId().isBlank())throw error(HttpStatus.FORBIDDEN,"School is not assigned");return u.getSchoolId();}
 private List<StudentEntity> children(UserEntity u){
  if(u.getRole()==Role.STUDENT)return students.findByEmailIgnoreCase(u.getEmail()).map(List::of).orElse(List.of());
  Map<Long,StudentEntity> result=new HashMap<>();
  students.findByGuardianEmailIgnoreCase(u.getEmail()).forEach(s->result.put(s.getId(),s));
  guardians.findByEmailIgnoreCase(u.getEmail()).ifPresent(g->{if(g.getStudentAdmissionNo()!=null)for(String a:g.getStudentAdmissionNo().split(","))students.findByAdmissionNoIgnoreCase(a.trim()).ifPresent(s->result.put(s.getId(),s));});
  return new ArrayList<>(result.values());
 }
 private boolean teaches(UserEntity u,StudentEntity s){return teachers.findByEmailIgnoreCase(u.getEmail()).map(t->t.getAssignedClasses().stream().anyMatch(c->Objects.toString(c.getName(),"").equalsIgnoreCase(Objects.toString(s.getClassName(),""))&&(c.getSection()==null||c.getSection().isBlank()||c.getSection().equalsIgnoreCase(s.getSection()))) || (t.getAssignedClass()!=null&&Arrays.stream(t.getAssignedClass().split(",")).anyMatch(a->a.trim().equalsIgnoreCase(s.getClassName())||a.trim().equalsIgnoreCase(s.getClassName()+" - "+s.getSection())))).orElse(false);}
 public boolean contact(UserEntity a,UserEntity b){
  if(!a.isEnabled()||!b.isEnabled()||Objects.equals(a.getId(),b.getId())||!school(a).equals(school(b)))return false;
  if(leader(a)||leader(b))return true;
  if(EMPLOYEES.contains(a.getRole())&&EMPLOYEES.contains(b.getRole()))return true;
  if(a.getRole()==Role.ACCOUNTANT||b.getRole()==Role.ACCOUNTANT)return true;
  var teacher=a.getRole()==Role.TEACHER?a:b.getRole()==Role.TEACHER?b:null;
  var family=teacher==a?b:a;
  if(teacher!=null&&(family.getRole()==Role.PARENT||family.getRole()==Role.STUDENT))return children(family).stream().anyMatch(s->teaches(teacher,s));
  if((a.getRole()==Role.PARENT&&b.getRole()==Role.STUDENT)||(b.getRole()==Role.PARENT&&a.getRole()==Role.STUDENT)){
   var parent=a.getRole()==Role.PARENT?a:b;var student=parent==a?b:a;
   return children(parent).stream().anyMatch(s->s.getEmail()!=null&&s.getEmail().equalsIgnoreCase(student.getEmail()));
  }
  return false;
 }
 public UserEntity user(Long id){return users.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"User not found"));}
 public List<Map<String,Object>> contacts(){var a=actor();return users.findBySchoolIdAndEnabledTrue(school(a)).stream().filter(u->contact(a,u)).map(u->Map.<String,Object>of("id",u.getId(),"name",u.getName(),"role",u.getRole().name())).toList();}
 public static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
