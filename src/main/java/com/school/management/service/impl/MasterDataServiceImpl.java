package com.school.management.service.impl;

import com.school.management.domain.user.Role;
import com.school.management.dto.MasterDataResponse;
import com.school.management.entity.*;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDataServiceImpl implements MasterDataService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public MasterDataResponse getMasterData(UserEntity currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return buildMasterDataResponse(currentUser);
    }

    private MasterDataResponse buildMasterDataResponse(UserEntity user) {
        Map<String, Object> userDetails = new LinkedHashMap<>();

        // 1. Base User identity fields
        userDetails.put("id", user.getId());
        userDetails.put("userId", user.getId());
        userDetails.put("email", user.getEmail());
        userDetails.put("name", user.getName() != null ? user.getName() : "");
        userDetails.put("role", user.getRole() != null ? user.getRole().name() : "");
        userDetails.put("userType", user.getRole() != null ? user.getRole().name() : "");
        userDetails.put("enabled", user.isEnabled());
        userDetails.put("createdAt", user.getCreatedAt());
        userDetails.put("updatedAt", user.getUpdatedAt());

        Role role = user.getRole();
        String userEmail = user.getEmail() != null ? user.getEmail().trim() : "";

        // 2. Role-specific enrichment
        if (role == Role.TEACHER) {
            enrichTeacherDetails(userDetails, user, userEmail);
        } else if (role == Role.STAFF || role == Role.PRINCIPAL || role == Role.ACCOUNTANT || role == Role.LIBRARIAN) {
            enrichStaffDetails(userDetails, user, userEmail);
        } else if (role == Role.STUDENT) {
            enrichStudentDetails(userDetails, user, userEmail);
        } else if (role == Role.PARENT) {
            enrichGuardianDetails(userDetails, user, userEmail);
        } else if (role == Role.ADMIN || role == Role.MASTER_ADMIN || role == Role.SUPER_ADMIN) {
            enrichAdminDetails(userDetails, user, userEmail);
        } else {
            enrichGeneralDetails(userDetails, user, userEmail);
        }

        // 3. Settings section (Future-ready: currently empty map as requested)
        Map<String, Object> settings = new LinkedHashMap<>();

        // 4. Data wrapper for nested compatibility
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userDetails", userDetails);
        data.put("settings", settings);

        return MasterDataResponse.builder()
                .success(true)
                .message("Master data retrieved successfully")
                .userDetails(userDetails)
                .settings(settings)
                .data(data)
                .build();
    }

    private void enrichTeacherDetails(Map<String, Object> userDetails, UserEntity user, String userEmail) {
        Optional<TeacherEntity> teacherOpt = teacherRepository.findByEmailIgnoreCase(userEmail);
        if (teacherOpt.isPresent()) {
            TeacherEntity t = teacherOpt.get();
            populateTeacherFields(userDetails, t);
        } else {
            userDetails.put("type", "Teacher");
            userDetails.put("designation", "Teacher");
            userDetails.put("department", "Faculty");
            userDetails.put("details", "Teacher • Faculty");
            userDetails.put("avatarUrl", "");
        }

        // Check if salary exists in staff records
        staffRepository.findByEmailIgnoreCase(userEmail).ifPresent(st -> {
            if (st.getSalary() != null) {
                userDetails.put("salary", st.getSalary());
            }
        });
    }

    private void populateTeacherFields(Map<String, Object> userDetails, TeacherEntity t) {
        userDetails.put("entityId", t.getId());
        userDetails.put("teacherId", t.getId());
        userDetails.put("employeeId", t.getEmployeeId());
        userDetails.put("firstName", t.getFirstName());
        userDetails.put("lastName", t.getLastName());
        String fullName = ((t.getFirstName() != null ? t.getFirstName() : "") + " "
                + (t.getLastName() != null ? t.getLastName() : "")).trim();
        if (!fullName.isEmpty()) {
            userDetails.put("name", fullName);
            userDetails.put("fullName", fullName);
        }
        userDetails.put("department", t.getDepartment());
        userDetails.put("departmentId", t.getDepartmentId());
        userDetails.put("subject", t.getSubject());
        userDetails.put("subjectIds", t.getSubjectIds());
        userDetails.put("qualification", t.getQualification());
        userDetails.put("designation", t.getDesignation() != null ? t.getDesignation() : "Teacher");
        userDetails.put("designationId", t.getDesignationId());
        userDetails.put("type", t.getType() != null ? t.getType() : "Teacher");
        userDetails.put("phone", t.getPhone());
        userDetails.put("address", t.getAddress());
        userDetails.put("joiningDate", t.getJoiningDate());
        userDetails.put("joinDate", t.getJoiningDate() != null ? t.getJoiningDate().toString() : null);
        userDetails.put("experienceYears", t.getExperienceYears());
        userDetails.put("bloodGroup", t.getBloodGroup());
        userDetails.put("jobType", t.getJobType() != null ? t.getJobType().name() : null);
        userDetails.put("avatar", t.getAvatar());
        userDetails.put("avatarUrl", t.getAvatar() != null ? t.getAvatar() : "");
        userDetails.put("status", t.getStatus() != null ? t.getStatus().name() : "ACTIVE");

        // Form fields
        userDetails.put("gender", t.getGender());
        userDetails.put("dob", t.getDob());
        userDetails.put("dateOfBirth", t.getDob());
        userDetails.put("fatherName", t.getFatherName());
        userDetails.put("motherName", t.getMotherName());
        userDetails.put("maritalStatus", t.getMaritalStatus());
        userDetails.put("contractType", t.getContractType());
        userDetails.put("shift", t.getShift());
        userDetails.put("workLocation", t.getWorkLocation());
        userDetails.put("height", t.getHeight());
        userDetails.put("weight", t.getWeight());
        userDetails.put("bankAccountNumber", t.getBankAccountNumber());
        userDetails.put("bankName", t.getBankName());
        userDetails.put("ifscCode", t.getIfscCode());
        userDetails.put("nationalIdNumber", t.getNationalIdNumber());
        userDetails.put("docName", t.getDocName());
        userDetails.put("prevSchoolName", t.getPrevSchoolName());
        userDetails.put("prevSchoolAddress", t.getPrevSchoolAddress());
        userDetails.put("permanentAddress", t.getPermanentAddress());
        userDetails.put("teacherBio", t.getTeacherBio());
        userDetails.put("bio", t.getTeacherBio());

        // Assigned Classes
        userDetails.put("assignedClass", t.getAssignedClass());
        userDetails.put("assignedClassIds", t.getAssignedClassIds());
        List<Map<String, Object>> classesList = new ArrayList<>();
        if (t.getAssignedClasses() != null) {
            try {
                for (SchoolClassEntity c : t.getAssignedClasses()) {
                    if (c != null) {
                        Map<String, Object> cmap = new LinkedHashMap<>();
                        cmap.put("id", c.getId());
                        cmap.put("name", c.getName());
                        cmap.put("section", c.getSection());
                        classesList.add(cmap);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (classesList.isEmpty() && t.getAssignedClassIds() != null && !t.getAssignedClassIds().isBlank()) {
            try {
                List<Long> ids = Arrays.stream(t.getAssignedClassIds().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .toList();
                List<SchoolClassEntity> found = schoolClassRepository.findAllById(ids);
                for (SchoolClassEntity c : found) {
                    Map<String, Object> cmap = new LinkedHashMap<>();
                    cmap.put("id", c.getId());
                    cmap.put("name", c.getName());
                    cmap.put("section", c.getSection());
                    classesList.add(cmap);
                }
            } catch (Exception ignored) {}
        }
        userDetails.put("assignedClasses", classesList);

        // Subject Specializations
        List<Map<String, Object>> subjectsList = new ArrayList<>();
        if (t.getSubjectSpecializations() != null) {
            try {
                for (SubjectEntity s : t.getSubjectSpecializations()) {
                    if (s != null) {
                        Map<String, Object> smap = new LinkedHashMap<>();
                        smap.put("id", s.getId());
                        smap.put("name", s.getName());
                        smap.put("code", s.getCode());
                        subjectsList.add(smap);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (subjectsList.isEmpty() && t.getSubjectIds() != null && !t.getSubjectIds().isBlank()) {
            try {
                List<Long> ids = Arrays.stream(t.getSubjectIds().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .toList();
                List<SubjectEntity> found = subjectRepository.findAllById(ids);
                for (SubjectEntity s : found) {
                    Map<String, Object> smap = new LinkedHashMap<>();
                    smap.put("id", s.getId());
                    smap.put("name", s.getName());
                    smap.put("code", s.getCode());
                    subjectsList.add(smap);
                }
            } catch (Exception ignored) {}
        }
        userDetails.put("subjects", subjectsList);
        userDetails.put("subjectSpecializations", subjectsList);

        // Convenience display details
        String desig = t.getDesignation() != null ? t.getDesignation() : "Teacher";
        String dept = t.getDepartment() != null ? t.getDepartment() : "Faculty";
        userDetails.put("details", desig + " • " + dept);
        userDetails.put("className", t.getSubject() != null && !t.getSubject().isBlank()
                ? t.getSubject() + " Teacher" : "Teacher");
    }

    private void enrichStaffDetails(Map<String, Object> userDetails, UserEntity user, String userEmail) {
        Optional<StaffEntity> staffOpt = staffRepository.findByEmailIgnoreCase(userEmail);
        if (staffOpt.isPresent()) {
            StaffEntity st = staffOpt.get();
            userDetails.put("entityId", st.getId());
            userDetails.put("staffId", st.getId());
            userDetails.put("name", st.getName());
            userDetails.put("fullName", st.getName());
            userDetails.put("phone", st.getPhone());
            userDetails.put("staffType", st.getStaffType());
            userDetails.put("type", st.getStaffType() != null ? st.getStaffType() : user.getRole().name());
            userDetails.put("designation", st.getDesignation() != null ? st.getDesignation() : user.getRole().name());
            userDetails.put("department", st.getStaffType() != null ? st.getStaffType() : "Administration");
            userDetails.put("salary", st.getSalary());
            userDetails.put("joinDate", st.getJoinDate());
            userDetails.put("joiningDate", st.getJoinDate());
            userDetails.put("status", st.getStatus() != null ? st.getStatus() : "Active");
            userDetails.put("role", st.getRole() != null ? st.getRole() : user.getRole().name());

            String desig = st.getDesignation() != null ? st.getDesignation() : user.getRole().name();
            String sType = st.getStaffType() != null ? st.getStaffType() : "Staff";
            userDetails.put("details", desig + " • " + sType);
            userDetails.put("className", desig);
        } else {
            userDetails.put("type", user.getRole().name());
            userDetails.put("designation", user.getRole().name());
            userDetails.put("department", "Administration");
            userDetails.put("details", user.getRole().name() + " • School Administration");
            userDetails.put("className", user.getRole().name());
            userDetails.put("status", "Active");
        }

        // Merge any extended profile fields if teacher entity exists for this user/email
        teacherRepository.findByEmailIgnoreCase(userEmail).ifPresent(t -> {
            if (!userDetails.containsKey("employeeId") || userDetails.get("employeeId") == null) {
                userDetails.put("employeeId", t.getEmployeeId());
            }
            if (t.getQualification() != null) userDetails.put("qualification", t.getQualification());
            if (t.getAvatar() != null && !t.getAvatar().isBlank()) {
                userDetails.put("avatar", t.getAvatar());
                userDetails.put("avatarUrl", t.getAvatar());
            }
            if (t.getGender() != null) userDetails.put("gender", t.getGender());
            if (t.getDob() != null) {
                userDetails.put("dob", t.getDob());
                userDetails.put("dateOfBirth", t.getDob());
            }
            if (t.getFatherName() != null) userDetails.put("fatherName", t.getFatherName());
            if (t.getMotherName() != null) userDetails.put("motherName", t.getMotherName());
            if (t.getMaritalStatus() != null) userDetails.put("maritalStatus", t.getMaritalStatus());
            if (t.getContractType() != null) userDetails.put("contractType", t.getContractType());
            if (t.getShift() != null) userDetails.put("shift", t.getShift());
            if (t.getWorkLocation() != null) userDetails.put("workLocation", t.getWorkLocation());
            if (t.getHeight() != null) userDetails.put("height", t.getHeight());
            if (t.getWeight() != null) userDetails.put("weight", t.getWeight());
            if (t.getBankAccountNumber() != null) userDetails.put("bankAccountNumber", t.getBankAccountNumber());
            if (t.getBankName() != null) userDetails.put("bankName", t.getBankName());
            if (t.getIfscCode() != null) userDetails.put("ifscCode", t.getIfscCode());
            if (t.getNationalIdNumber() != null) userDetails.put("nationalIdNumber", t.getNationalIdNumber());
            if (t.getDocName() != null) userDetails.put("docName", t.getDocName());
            if (t.getPrevSchoolName() != null) userDetails.put("prevSchoolName", t.getPrevSchoolName());
            if (t.getPrevSchoolAddress() != null) userDetails.put("prevSchoolAddress", t.getPrevSchoolAddress());
            if (t.getPermanentAddress() != null) userDetails.put("permanentAddress", t.getPermanentAddress());
            if (t.getAddress() != null) userDetails.put("address", t.getAddress());
            if (t.getTeacherBio() != null) {
                userDetails.put("teacherBio", t.getTeacherBio());
                userDetails.put("bio", t.getTeacherBio());
            }
            if (t.getBloodGroup() != null) userDetails.put("bloodGroup", t.getBloodGroup());
            if (t.getExperienceYears() != null) userDetails.put("experienceYears", t.getExperienceYears());
        });

        if (!userDetails.containsKey("avatarUrl") || userDetails.get("avatarUrl") == null) {
            userDetails.put("avatarUrl", "");
        }
    }

    private void enrichStudentDetails(Map<String, Object> userDetails, UserEntity user, String userEmail) {
        Optional<StudentEntity> studentOpt = studentRepository.findByEmailIgnoreCase(userEmail);
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByPhone(userEmail);
        }
        if (studentOpt.isPresent()) {
            StudentEntity s = studentOpt.get();
            userDetails.put("entityId", s.getId());
            userDetails.put("studentId", s.getId());
            userDetails.put("admissionNo", s.getAdmissionNo());
            userDetails.put("rollNo", s.getRollNo());
            userDetails.put("name", s.getName());
            userDetails.put("fullName", s.getName());
            userDetails.put("firstName", s.getFirstName());
            userDetails.put("lastName", s.getLastName());
            userDetails.put("className", s.getClassName());
            userDetails.put("section", s.getSection());
            userDetails.put("category", s.getCategory());
            userDetails.put("academicYear", s.getAcademicYear());
            userDetails.put("gender", s.getGender() != null ? s.getGender().name() : null);
            userDetails.put("dob", s.getDateOfBirth());
            userDetails.put("dateOfBirth", s.getDateOfBirth());
            userDetails.put("phone", s.getPhone());
            userDetails.put("studentPhoto", s.getStudentPhoto());
            userDetails.put("avatar", s.getStudentPhoto());
            userDetails.put("avatarUrl", s.getStudentPhoto() != null ? s.getStudentPhoto() : "");
            userDetails.put("fatherName", s.getFatherName());
            userDetails.put("fatherPhone", s.getFatherPhone());
            userDetails.put("fatherOccupation", s.getFatherOccupation());
            userDetails.put("fatherPhoto", s.getFatherPhoto());
            userDetails.put("motherName", s.getMotherName());
            userDetails.put("motherPhone", s.getMotherPhone());
            userDetails.put("motherOccupation", s.getMotherOccupation());
            userDetails.put("motherPhoto", s.getMotherPhoto());
            userDetails.put("guardianRelation", s.getGuardianRelation());
            userDetails.put("guardianName", s.getGuardianName());
            userDetails.put("guardianEmail", s.getGuardianEmail());
            userDetails.put("guardianPhone", s.getGuardianPhone());
            userDetails.put("guardianOccupation", s.getGuardianOccupation());
            userDetails.put("guardianAddress", s.getGuardianAddress());
            userDetails.put("guardianPhoto", s.getGuardianPhoto());
            userDetails.put("bloodGroup", s.getBloodGroup());
            userDetails.put("height", s.getHeight());
            userDetails.put("weight", s.getWeight());
            userDetails.put("bankAccountNumber", s.getBankAccountNumber());
            userDetails.put("bankName", s.getBankName());
            userDetails.put("ifscCode", s.getIfscCode());
            userDetails.put("nationalIdNumber", s.getNationalIdNumber());
            userDetails.put("prevSchoolName", s.getPrevSchoolName());
            userDetails.put("prevSchoolAddress", s.getPrevSchoolAddress());
            userDetails.put("currentAddress", s.getCurrentAddress());
            userDetails.put("permanentAddress", s.getPermanentAddress());
            userDetails.put("address", s.getCurrentAddress() != null ? s.getCurrentAddress() : s.getPermanentAddress());
            userDetails.put("hostelName", s.getHostelName());
            userDetails.put("roomNo", s.getRoomNo());
            userDetails.put("docName", s.getDocName());
            userDetails.put("docFile", s.getDocFile());
            userDetails.put("studentNotes", s.getStudentNotes());
            userDetails.put("attendancePercentage", s.getAttendancePercentage());
            userDetails.put("status", s.getStatus() != null ? s.getStatus().name() : "ACTIVE");

            String rollText = (s.getRollNo() != null && !s.getRollNo().isBlank()) ? "Roll No: " + s.getRollNo() : "";
            String classText = (s.getClassName() != null && !s.getClassName().isBlank()) ? s.getClassName() : "";
            String detailsStr = rollText + (!rollText.isEmpty() && !classText.isEmpty() ? " • " : "") + classText;
            userDetails.put("details", detailsStr.isEmpty() ? "Student" : detailsStr);
        } else {
            userDetails.put("details", "Student");
            userDetails.put("avatarUrl", "");
        }
    }

    private void enrichGuardianDetails(Map<String, Object> userDetails, UserEntity user, String userEmail) {
        Optional<GuardianEntity> guardianOpt = guardianRepository.findByEmailIgnoreCase(userEmail);
        if (guardianOpt.isEmpty()) {
            guardianOpt = guardianRepository.findByPhone(userEmail);
        }
        if (guardianOpt.isPresent()) {
            GuardianEntity g = guardianOpt.get();
            userDetails.put("entityId", g.getId());
            userDetails.put("guardianId", g.getId());
            userDetails.put("name", g.getName());
            userDetails.put("fullName", g.getName());
            userDetails.put("phone", g.getPhone());
            userDetails.put("guardianType", g.getGuardianType() != null ? g.getGuardianType().name() : "PARENT");
            userDetails.put("type", "Parent");
            userDetails.put("occupation", g.getOccupation());
            userDetails.put("address", g.getAddress());
            userDetails.put("photo", g.getPhoto());
            userDetails.put("avatar", g.getPhoto());
            userDetails.put("avatarUrl", g.getPhoto() != null ? g.getPhoto() : "");
            userDetails.put("feeStatus", g.getFeeStatus());
            userDetails.put("studentAdmissionNo", g.getStudentAdmissionNo());

            String childAdm = g.getStudentAdmissionNo();
            if (childAdm != null && !childAdm.isBlank()) {
                studentRepository.findByAdmissionNoIgnoreCase(childAdm).ifPresent(child -> {
                    userDetails.put("childName", child.getName());
                    userDetails.put("childClassName", child.getClassName());
                    userDetails.put("childSection", child.getSection());
                    userDetails.put("childRollNo", child.getRollNo());
                    userDetails.put("childAvatarUrl", child.getStudentPhoto());
                    userDetails.put("className", "Parent of " + child.getName() + " (" + child.getClassName() + ")");
                    userDetails.put("details", "Parent of " + child.getName() + " (" + child.getClassName() + ")");
                });
            }
            if (!userDetails.containsKey("details")) {
                userDetails.put("className", "Parent");
                userDetails.put("details", "Guardian (" + (g.getGuardianType() != null ? g.getGuardianType().name() : "Parent") + ")");
            }
        } else {
            userDetails.put("className", "Parent");
            userDetails.put("details", "Guardian");
            userDetails.put("avatarUrl", "");
        }
    }

    private void enrichAdminDetails(Map<String, Object> userDetails, UserEntity user, String userEmail) {
        String desig = (user.getRole() == Role.MASTER_ADMIN || user.getRole() == Role.SUPER_ADMIN)
                ? "Master Admin" : "System Administrator";
        userDetails.put("designation", desig);
        userDetails.put("department", "Administration");
        userDetails.put("type", user.getRole().name());
        userDetails.put("className", desig);
        userDetails.put("details", desig);
        userDetails.put("avatarUrl", "");

        teacherRepository.findByEmailIgnoreCase(userEmail).ifPresent(t -> {
            populateTeacherFields(userDetails, t);
            userDetails.put("designation", desig);
        });
        staffRepository.findByEmailIgnoreCase(userEmail).ifPresent(st -> {
            if (st.getSalary() != null) userDetails.put("salary", st.getSalary());
        });
    }

    private void enrichGeneralDetails(Map<String, Object> userDetails, UserEntity user, String userEmail) {
        userDetails.put("designation", user.getRole() != null ? user.getRole().name() : "User");
        userDetails.put("department", "General");
        userDetails.put("details", "School Desk User");
        userDetails.put("avatarUrl", "");
    }
}
