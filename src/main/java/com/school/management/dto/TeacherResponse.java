package com.school.management.dto;

import com.school.management.domain.teacher.JobType;
import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.entity.SchoolClassEntity;
import com.school.management.entity.SubjectEntity;
import com.school.management.entity.TeacherEntity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherResponse {

    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String department;
    private Long departmentId;
    private String subject;
    private String qualification;
    private String designation;
    private Long designationId;
    private String phone;
    private String email;
    private String address;
    private LocalDate joiningDate;
    private Integer experienceYears;
    private String bloodGroup;
    private JobType jobType;
    private String avatar;
    private TeacherStatus status;
    private String type;

    // Multi-select Assigned Classes
    private List<Long> assignedClassIds;
    private List<Long> classIds; // alias
    private String assignedClass;
    private List<SchoolClassResponse> assignedClasses;

    // Multi-select Subject Specializations
    private List<Long> subjectIds;
    private List<SubjectResponse> subjects;
    private List<SubjectResponse> subjectSpecializations;

    // Form fields
    private String gender;
    private LocalDate dob;
    private String fatherName;
    private String motherName;
    private String maritalStatus;
    private String contractType;
    private String shift;
    private String workLocation;
    private String height;
    private String weight;
    private String bankAccountNumber;
    private String bankName;
    private String ifscCode;
    private String nationalIdNumber;
    private String docName;
    private String prevSchoolName;
    private String prevSchoolAddress;
    private String permanentAddress;
    private String teacherBio;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TeacherResponse fromEntity(TeacherEntity teacher) {
        if (teacher == null) return null;
        TeacherResponse res = new TeacherResponse();
        res.setId(teacher.getId());
        res.setEmployeeId(teacher.getEmployeeId());
        res.setFirstName(teacher.getFirstName());
        res.setLastName(teacher.getLastName());
        res.setFullName(((teacher.getFirstName() != null ? teacher.getFirstName() : "") + " " + (teacher.getLastName() != null ? teacher.getLastName() : "")).trim());
        res.setDepartment(teacher.getDepartment());
        res.setDepartmentId(teacher.getDepartmentId());
        res.setSubject(teacher.getSubject());
        res.setQualification(teacher.getQualification());
        res.setDesignation(teacher.getDesignation());
        res.setDesignationId(teacher.getDesignationId());
        res.setPhone(teacher.getPhone());
        res.setEmail(teacher.getEmail());
        res.setAddress(teacher.getAddress());
        res.setJoiningDate(teacher.getJoiningDate());
        res.setExperienceYears(teacher.getExperienceYears());
        res.setBloodGroup(teacher.getBloodGroup());
        res.setJobType(teacher.getJobType());
        res.setAvatar(teacher.getAvatar());
        res.setStatus(teacher.getStatus());
        res.setType(teacher.getType() != null && !teacher.getType().isBlank() ? teacher.getType() : "Teacher");

        // Form fields
        res.setGender(teacher.getGender());
        res.setDob(teacher.getDob());
        res.setFatherName(teacher.getFatherName());
        res.setMotherName(teacher.getMotherName());
        res.setMaritalStatus(teacher.getMaritalStatus());
        res.setContractType(teacher.getContractType());
        res.setShift(teacher.getShift());
        res.setWorkLocation(teacher.getWorkLocation());
        res.setHeight(teacher.getHeight());
        res.setWeight(teacher.getWeight());
        res.setBankAccountNumber(teacher.getBankAccountNumber());
        res.setBankName(teacher.getBankName());
        res.setIfscCode(teacher.getIfscCode());
        res.setNationalIdNumber(teacher.getNationalIdNumber());
        res.setDocName(teacher.getDocName());
        res.setPrevSchoolName(teacher.getPrevSchoolName());
        res.setPrevSchoolAddress(teacher.getPrevSchoolAddress());
        res.setPermanentAddress(teacher.getPermanentAddress());
        res.setTeacherBio(teacher.getTeacherBio());
        res.setAssignedClass(teacher.getAssignedClass());

        // Process Assigned Classes
        List<Long> classIds = new ArrayList<>();
        List<SchoolClassResponse> classResponses = new ArrayList<>();
        if (teacher.getAssignedClasses() != null) {
            try {
                for (SchoolClassEntity c : teacher.getAssignedClasses()) {
                    if (c != null) {
                        classIds.add(c.getId());
                        classResponses.add(SchoolClassResponse.fromEntity(c));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (classIds.isEmpty() && teacher.getAssignedClassIds() != null && !teacher.getAssignedClassIds().isBlank()) {
            for (String p : teacher.getAssignedClassIds().split(",")) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    try {
                        classIds.add(Long.parseLong(clean));
                    } catch (Exception ignored) {}
                }
            }
        }
        res.setAssignedClassIds(classIds);
        res.setClassIds(classIds);
        res.setAssignedClasses(classResponses);

        // Process Subject Specializations
        List<Long> subjectIds = new ArrayList<>();
        List<SubjectResponse> subjectResponses = new ArrayList<>();
        if (teacher.getSubjectSpecializations() != null) {
            try {
                for (SubjectEntity s : teacher.getSubjectSpecializations()) {
                    if (s != null) {
                        subjectIds.add(s.getId());
                        subjectResponses.add(SubjectResponse.fromEntity(s));
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (subjectIds.isEmpty() && teacher.getSubjectIds() != null && !teacher.getSubjectIds().isBlank()) {
            for (String p : teacher.getSubjectIds().split(",")) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    try {
                        subjectIds.add(Long.parseLong(clean));
                    } catch (Exception ignored) {}
                }
            }
        }
        res.setSubjectIds(subjectIds);
        res.setSubjects(subjectResponses);
        res.setSubjectSpecializations(subjectResponses);

        res.setCreatedAt(teacher.getCreatedAt());
        res.setUpdatedAt(teacher.getUpdatedAt());
        return res;
    }
}