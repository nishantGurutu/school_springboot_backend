package com.school.management.service;

import com.school.management.entity.*;
import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class StudentTeachersService {
    private final TeacherRepository teachers;
    private final SchoolClassRepository classes;
    private final SectionRepository sections;

    public List<Map<String,Object>> getTeachers(StudentEntity student) {
        if (student.getClassName()==null || student.getClassName().isBlank()) return List.of();
        return teachers.findAll().stream()
                .filter(t -> t.getType()==null || t.getType().equalsIgnoreCase("Teacher"))
                .filter(t -> t.getStatus()==TeacherStatus.ACTIVE || t.getStatus()==TeacherStatus.ON_LEAVE)
                .filter(t -> assigned(t,student))
                .sorted(Comparator.comparing(t -> Objects.toString(t.getFirstName(),"")+" "+Objects.toString(t.getLastName(),""), String.CASE_INSENSITIVE_ORDER))
                .map(t -> dto(t,student)).toList();
    }

    private boolean matches(SchoolClassEntity assigned, StudentEntity student) {
        String section=assigned.getSection();
        if ((section==null || section.isBlank()) && assigned.getSectionId()!=null) {
            section=sections.findById(assigned.getSectionId()).map(SectionEntity::getName).orElse(null);
            if (section==null) return false;
        }
        return student.getClassName().trim().equalsIgnoreCase(assigned.getName())
                && (section==null || section.isBlank()
                    || section.trim().equalsIgnoreCase(Objects.toString(student.getSection(),"").trim()));
    }

    private boolean assigned(TeacherEntity teacher,StudentEntity student) {
        if (!teacher.getAssignedClasses().isEmpty())
            return teacher.getAssignedClasses().stream().anyMatch(c -> matches(c,student));
        if (teacher.getAssignedClassIds()!=null && !teacher.getAssignedClassIds().isBlank()) {
            List<Long> ids=Arrays.stream(teacher.getAssignedClassIds().split(",")).map(String::trim)
                    .filter(id -> id.matches("\\d+")).map(Long::valueOf).toList();
            return classes.findAllById(ids).stream().anyMatch(c -> matches(c,student));
        }
        String name=student.getClassName().trim();
        String section=Objects.toString(student.getSection(),"").trim();
        return Arrays.stream(Objects.toString(teacher.getAssignedClass(),"").split(",")).map(String::trim)
                .anyMatch(value -> value.equalsIgnoreCase(name) || !section.isEmpty() &&
                        (value.equalsIgnoreCase(name+" ("+section+")") || value.equalsIgnoreCase(name+" - "+section)));
    }

    private Map<String,Object> dto(TeacherEntity teacher,StudentEntity student) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",teacher.getId());
        result.put("name",(Objects.toString(teacher.getFirstName(),"")+" "+Objects.toString(teacher.getLastName(),"")).trim());
        List<String> subjects=teacher.getSubjectSpecializations().stream()
                .filter(s -> s.getClassName()==null || s.getClassName().isBlank() || s.getClassName().trim().equalsIgnoreCase(student.getClassName().trim()))
                .map(SubjectEntity::getName).distinct().sorted().toList();
        result.put("subject",subjects.isEmpty()?teacher.getSubject():String.join(", ",subjects));
        result.put("email",teacher.getEmail());result.put("phone",teacher.getPhone());
        result.put("designation",teacher.getDesignation());result.put("avatar",teacher.getAvatar());
        return result;
    }
}
