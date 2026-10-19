package com.school.management.service;

import com.school.management.domain.user.Role;
import com.school.management.entity.SchoolClassEntity;
import com.school.management.entity.StudentEntity;
import com.school.management.entity.TeacherEntity;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.GuardianRepository;
import com.school.management.repository.SchoolClassRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.repository.TeacherRepository;
import com.school.management.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PortalAccessService {
    private final SecurityUtil security;
    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final TeacherRepository teachers;
    private final SchoolClassRepository classes;
    public UserEntity actor() { return security.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated")); }
    public boolean manager() { return Set.of(Role.TEACHER,Role.ADMIN,Role.MASTER_ADMIN,Role.SUPER_ADMIN,Role.PRINCIPAL).contains(actor().getRole()); }
    public void requireManager() { if (!manager()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Teacher access required"); }
    public List<StudentEntity> children() {
        UserEntity user=actor();
        if (user.getRole()==Role.STUDENT) return List.of(students.findByEmailIgnoreCase(user.getEmail()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile not linked")));
        if (user.getRole()!=Role.PARENT) return List.of();
        Map<Long,StudentEntity> linked=new LinkedHashMap<>();
        students.findByGuardianEmailIgnoreCase(user.getEmail()).forEach(s -> linked.put(s.getId(),s));
        guardians.findByEmailIgnoreCase(user.getEmail()).ifPresent(g -> {
            if(g.getStudentAdmissionNo()!=null) for(String adm:g.getStudentAdmissionNo().split(","))
                students.findByAdmissionNoIgnoreCase(adm.trim()).ifPresent(s -> linked.put(s.getId(),s));
        });
        return new ArrayList<>(linked.values());
    }
    public boolean teaches(String name,String section) {
        if(actor().getRole()!=Role.TEACHER) return manager();
        TeacherEntity t=teachers.findByEmailIgnoreCase(actor().getEmail()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Teacher profile not linked"));
        if(t.getAssignedClasses().stream().anyMatch(c -> c.getName().equalsIgnoreCase(name) && (c.getSection()==null || c.getSection().isBlank() || c.getSection().equalsIgnoreCase(section)))) return true;
        return t.getAssignedClass()!=null && Arrays.stream(t.getAssignedClass().split(",")).anyMatch(c -> c.trim().equalsIgnoreCase(name) || c.trim().equalsIgnoreCase(name+" - "+section));
    }
    public void requireClass(String name,String section) {
        requireManager();
        if(name==null || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Class is required");
        if(!teaches(name,section)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Class is not assigned to this teacher");
        if(classes.findAll().stream().noneMatch(c -> c.getName().equalsIgnoreCase(name) && (section==null || section.isBlank() || section.equalsIgnoreCase(c.getSection()))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Class/section does not exist");
    }
    public StudentEntity student(String admission) {
        if(manager()) {
            StudentEntity s=students.findByAdmissionNoIgnoreCase(Objects.toString(admission,"").trim()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"));
            if(!teaches(s.getClassName(),s.getSection())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Student is not in an assigned class");
            return s;
        }
        List<StudentEntity> linked=children();
        if(admission==null || admission.isBlank()) {
            if(linked.size()==1) return linked.get(0);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select a linked child");
        }
        return linked.stream().filter(s -> admission.trim().equalsIgnoreCase(s.getAdmissionNo())).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Student is not linked to your account"));
    }
    public List<SchoolClassEntity> classes() { requireManager(); return classes.findAll().stream().filter(c -> teaches(c.getName(),c.getSection())).toList(); }
    public List<StudentEntity> eligible(String name,String section) { requireClass(name,section); return students.findByClassNameIgnoreCase(name).stream().filter(s -> section==null || section.isBlank() || section.equalsIgnoreCase(s.getSection())).toList(); }
}
