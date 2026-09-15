package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.school.management.domain.student.Gender;
import com.school.management.domain.student.StudentStatus;
import com.school.management.entity.StudentEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class StudentResponse {

    // --- Dashboard table columns ---
    private Long id;
    private String admissionNo;
    private String name;                    // full name
    private String rollNo;
    private String className;
    private String section;
    private Gender gender;
    private String dob;                     // dd/MM/yyyy
    private String guardian;                // guardian name
    private String phone;
    private String email;
    private String attendance;              // e.g. "98.5%"
    private String status;                  // e.g. "Active"
    private String avatar;                  // photo url

    // --- Full details ---
    private String category;
    private String academicYear;
    private String studentPhoto;
    private String fatherName;
    private String fatherPhone;
    private String fatherOccupation;
    private String fatherPhoto;
    private String motherName;
    private String motherPhone;
    private String motherOccupation;
    private String motherPhoto;
    private String guardianRelation;
    private String guardianName;
    private String guardianEmail;
    private String guardianPhone;
    private String guardianOccupation;
    private String guardianAddress;
    private String guardianPhoto;
    private String bloodGroup;
    private String height;
    private String weight;
    private String bankAccountNumber;
    private String bankName;
    private String ifscCode;
    private String nationalIdNumber;
    private String prevSchoolName;
    private String prevSchoolAddress;
    private String currentAddress;
    private String permanentAddress;
    private String hostelName;
    private String roomNo;
    private String docName;
    private String docFile;
    private String studentNotes;
    private Double attendancePercentage;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("class")
    public String getClassNameCombined() {
        if (section == null || section.isBlank()) {
            return className;
        }
        return className + " - " + section;
    }

    public static StudentResponse fromEntity(StudentEntity s) {
        String fullName = s.getName() != null ? s.getName()
                : (s.getFirstName() + " " + s.getLastName()).trim();
        return StudentResponse.builder()
                .id(s.getId())
                .admissionNo(s.getAdmissionNo())
                .name(fullName)
                .rollNo(s.getRollNo())
                .className(s.getClassName())
                .section(s.getSection() == null ? "" : s.getSection())
                .gender(s.getGender())
                .dob(s.getDateOfBirth() == null ? null
                        : s.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .guardian(s.getGuardianName())
                .phone(s.getPhone())
                .email(s.getEmail())
                .attendance(s.getAttendancePercentage() == null ? null
                        : s.getAttendancePercentage() + "%")
                .status(s.getStatus() == null ? null : capitalize(s.getStatus().name()))
                .avatar(s.getStudentPhoto())
                .category(s.getCategory())
                .academicYear(s.getAcademicYear())
                .studentPhoto(s.getStudentPhoto())
                .fatherName(s.getFatherName())
                .fatherPhone(s.getFatherPhone())
                .fatherOccupation(s.getFatherOccupation())
                .fatherPhoto(s.getFatherPhoto())
                .motherName(s.getMotherName())
                .motherPhone(s.getMotherPhone())
                .motherOccupation(s.getMotherOccupation())
                .motherPhoto(s.getMotherPhoto())
                .guardianRelation(s.getGuardianRelation())
                .guardianName(s.getGuardianName())
                .guardianEmail(s.getGuardianEmail())
                .guardianPhone(s.getGuardianPhone())
                .guardianOccupation(s.getGuardianOccupation())
                .guardianAddress(s.getGuardianAddress())
                .guardianPhoto(s.getGuardianPhoto())
                .bloodGroup(s.getBloodGroup())
                .height(s.getHeight())
                .weight(s.getWeight())
                .bankAccountNumber(s.getBankAccountNumber())
                .bankName(s.getBankName())
                .ifscCode(s.getIfscCode())
                .nationalIdNumber(s.getNationalIdNumber())
                .prevSchoolName(s.getPrevSchoolName())
                .prevSchoolAddress(s.getPrevSchoolAddress())
                .currentAddress(s.getCurrentAddress())
                .permanentAddress(s.getPermanentAddress())
                .hostelName(s.getHostelName())
                .roomNo(s.getRoomNo())
                .docName(s.getDocName())
                .docFile(s.getDocFile())
                .studentNotes(s.getStudentNotes())
                .attendancePercentage(s.getAttendancePercentage())
                .dateOfBirth(s.getDateOfBirth())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}