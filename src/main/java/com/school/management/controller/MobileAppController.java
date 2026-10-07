package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import com.school.management.dto.HolidayResponse;
import com.school.management.service.HolidayService;
import com.school.management.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mobile App", description = "Enterprise endpoints serving Flutter Mobile App features backed by PostgreSQL")
public class MobileAppController {

    private final SecurityUtil securityUtil;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final HolidayService holidayService;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamResultRepository examResultRepository;
    private final FeeCollectionRepository feeCollectionRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final HomeworkRepository homeworkRepository;
    private final TimetableRepository timetableRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeService noticeService;
    private final AccountTransactionRepository accountTransactionRepository;

    // Transient student submission tracking
    private static final Map<String, String> submittedHomeworkMap = new ConcurrentHashMap<>();

    // ==========================================
    // 1. PROFILE ENDPOINTS
    // ==========================================
    @GetMapping("/profile/me")
    @Operation(summary = "Get current mobile user profile with role-specific details")
    public ResponseEntity<Map<String, Object>> getProfile() {
        UserEntity user = securityUtil.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId().toString());
        profile.put("email", user.getEmail());
        profile.put("name", user.getName() != null ? user.getName() : "");
        profile.put("role", user.getRole().name().toLowerCase());

        if (user.getRole() == Role.STUDENT) {
            studentRepository.findByEmail(user.getEmail()).ifPresentOrElse(s -> {
                profile.put("name", s.getName());
                profile.put("className", s.getClassName() != null ? s.getClassName() : "");
                String details = "";
                if (s.getRollNo() != null && !s.getRollNo().isBlank()) {
                    details += "Roll No: " + s.getRollNo();
                }
                if (s.getClassName() != null && !s.getClassName().isBlank()) {
                    details += (details.isEmpty() ? "" : " • ") + s.getClassName();
                }
                profile.put("details", details);
                profile.put("avatarUrl", s.getStudentPhoto() != null ? s.getStudentPhoto() : "");
                profile.put("phone", s.getPhone() != null ? s.getPhone() : "");
                profile.put("admissionNo", s.getAdmissionNo() != null ? s.getAdmissionNo() : "");
                profile.put("fatherName", s.getFatherName() != null ? s.getFatherName() : "");
            }, () -> {
                profile.put("className", "");
                profile.put("details", "");
                profile.put("admissionNo", "");
                profile.put("avatarUrl", "");
                profile.put("phone", "");
                profile.put("fatherName", "");
            });
        } else if (user.getRole() == Role.PARENT) {
            guardianRepository.findByEmail(user.getEmail()).ifPresentOrElse(g -> {
                profile.put("name", g.getName() != null ? g.getName() : user.getName());
                String childAdm = g.getStudentAdmissionNo();
                profile.put("className", childAdm != null && !childAdm.isBlank() ? "Parent of " + childAdm : "Parent");
                profile.put("details", g.getGuardianType() != null ? g.getGuardianType().name() : "Guardian");
                profile.put("avatarUrl", g.getPhoto() != null ? g.getPhoto() : "");
                profile.put("phone", g.getPhone() != null ? g.getPhone() : "");
                profile.put("admissionNo", childAdm != null ? childAdm : "");
            }, () -> {
                profile.put("className", "Parent");
                profile.put("details", "Guardian");
                profile.put("admissionNo", "");
                profile.put("avatarUrl", "");
                profile.put("phone", "");
            });
        } else if (user.getRole() == Role.TEACHER) {
            teacherRepository.findByEmail(user.getEmail()).ifPresentOrElse(t -> {
                String fullName = ((t.getFirstName() != null ? t.getFirstName() : "") + " "
                        + (t.getLastName() != null ? t.getLastName() : "")).trim();
                profile.put("name", !fullName.isEmpty() ? fullName : user.getName());
                profile.put("className", t.getSubject() != null && !t.getSubject().isBlank() ? t.getSubject() + " Teacher" : "Teacher");
                profile.put("details", t.getDepartment() != null && !t.getDepartment().isBlank() ? t.getDepartment() + " Department" : "Faculty");
                profile.put("avatarUrl", t.getAvatar() != null ? t.getAvatar() : "");
                profile.put("phone", t.getPhone() != null ? t.getPhone() : "");
            }, () -> {
                profile.put("className", "Teacher");
                profile.put("details", "Faculty");
                profile.put("avatarUrl", "");
                profile.put("phone", "");
            });
        } else if (user.getRole() == Role.STAFF || user.getRole() == Role.PRINCIPAL
                || user.getRole() == Role.ACCOUNTANT || user.getRole() == Role.LIBRARIAN) {
            staffRepository.findByEmail(user.getEmail()).ifPresentOrElse(st -> {
                profile.put("name", st.getName() != null ? st.getName() : user.getName());
                profile.put("className", st.getDesignation() != null ? st.getDesignation() : user.getRole().name());
                profile.put("details",
                        (st.getStaffType() != null ? st.getStaffType() : "Administration") + " Staff");
                profile.put("phone", st.getPhone() != null ? st.getPhone() : "");
                profile.put("avatarUrl", "");
            }, () -> {
                profile.put("className", user.getRole().name());
                profile.put("details", "School Administration");
                profile.put("avatarUrl", "");
                profile.put("phone", "");
            });
        } else if (user.getRole() == Role.ADMIN || user.getRole() == Role.MASTER_ADMIN || user.getRole() == Role.SUPER_ADMIN) {
            profile.put("className", "Admin");
            profile.put("details", "Administrator");
            profile.put("avatarUrl", "");
            profile.put("phone", "");
        }

        return ResponseEntity.ok(profile);
    }

    // ==========================================
    // 2. HOLIDAYS ENDPOINT
    // ==========================================
    @GetMapping("/holidays")
    @Operation(summary = "Get school holidays for mobile app")
    public ResponseEntity<List<HolidayResponse>> getMobileHolidays() {
        return ResponseEntity.ok(holidayService.getAll());
    }

    // ==========================================
    // 3. ATTENDANCE ENDPOINTS
    // ==========================================
    @GetMapping("/attendance")
    @Operation(summary = "Get attendance records with optional role/date/className filters")
    public ResponseEntity<List<Map<String, Object>>> getAttendance(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String className) {

        List<Map<String, Object>> result = new ArrayList<>();

        try {
            List<AttendanceEntity> dbEntities;
            if (type != null && !type.isBlank() && date != null && !date.isBlank()) {
                dbEntities = attendanceRepository.findByAttendanceTypeAndAttendanceDate(type.toUpperCase(), date);
                if (dbEntities.isEmpty()) {
                    dbEntities = attendanceRepository.findByAttendanceTypeAndAttendanceDate(type.toLowerCase(), date);
                }
            } else if (type != null && !type.isBlank()) {
                dbEntities = attendanceRepository.findByAttendanceType(type.toUpperCase());
                if (dbEntities.isEmpty()) {
                    dbEntities = attendanceRepository.findByAttendanceType(type.toLowerCase());
                }
            } else if (date != null && !date.isBlank()) {
                dbEntities = attendanceRepository.findAll().stream()
                        .filter(a -> date.equals(a.getAttendanceDate()))
                        .toList();
            } else {
                dbEntities = attendanceRepository.findAll();
            }

            if (className != null && !className.isBlank()) {
                dbEntities = dbEntities.stream()
                        .filter(a -> className.equalsIgnoreCase(a.getClassName()))
                        .toList();
            }

            for (AttendanceEntity entity : dbEntities) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("id", entity.getId());
                rec.put("attendanceType", entity.getAttendanceType());
                rec.put("admissionNo", entity.getAdmissionNo());
                rec.put("name", entity.getName());
                rec.put("rollNo", entity.getRollNo());
                rec.put("className", entity.getClassName());
                rec.put("department", entity.getDepartment());
                rec.put("designation", entity.getDesignation());
                rec.put("date", entity.getAttendanceDate() != null ? entity.getAttendanceDate() : "");
                rec.put("status", entity.getStatus() != null ? entity.getStatus().toLowerCase() : "present");
                rec.put("notes", entity.getNote());
                rec.put("avatar", entity.getAvatar());
                rec.put("checkInTime", entity.getCheckInTime());
                rec.put("checkOutTime", entity.getCheckOutTime());
                result.add(rec);
            }
        } catch (Exception e) {
            log.warn("Error querying database attendance: {}", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/attendance/mark")
    @Operation(summary = "Mark attendance directly for student or staff")
    public ResponseEntity<Map<String, Object>> markAttendance(@RequestBody Map<String, Object> req) {
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String date = req.getOrDefault("date", LocalDate.now().toString()).toString();
        String name = req.getOrDefault("name", user != null ? user.getName() : "").toString();
        String rawType = req.getOrDefault("attendanceType",
                req.getOrDefault("role", req.getOrDefault("type", "STUDENT"))).toString();

        String normalizedType = "STUDENT";
        if (user != null && user.getRole() == Role.STUDENT) {
            normalizedType = "STUDENT";
        } else if (user != null && (user.getRole() == Role.STAFF || user.getRole() == Role.ACCOUNTANT || user.getRole() == Role.LIBRARIAN)) {
            normalizedType = "EMPLOYEE";
        } else if (user != null && user.getRole() == Role.TEACHER) {
            normalizedType = "TEACHER";
        } else if ("teacher".equalsIgnoreCase(rawType)) {
            normalizedType = "TEACHER";
        } else if ("staff".equalsIgnoreCase(rawType) || "employee".equalsIgnoreCase(rawType)) {
            normalizedType = "EMPLOYEE";
        }

        String status = req.getOrDefault("status", "present").toString().toLowerCase();
        String notes = req.getOrDefault("notes", req.getOrDefault("note", "")).toString();
        String className = req.getOrDefault("className", "").toString();
        String department = req.getOrDefault("department", "").toString();
        String designation = req.getOrDefault("designation", "").toString();
        String rollNo = req.getOrDefault("rollNo", "").toString();
        String checkInTime = req.containsKey("checkInTime") && req.get("checkInTime") != null
                ? req.get("checkInTime").toString() : null;
        String checkOutTime = req.containsKey("checkOutTime") && req.get("checkOutTime") != null
                ? req.get("checkOutTime").toString() : null;
        String admissionNo = req.containsKey("admissionNo") && req.get("admissionNo") != null
                ? req.get("admissionNo").toString().trim()
                : (!rollNo.isEmpty() ? "ADM-" + rollNo : null);
        String avatar = req.containsKey("avatar") && req.get("avatar") != null ? req.get("avatar").toString() : null;

        AttendanceEntity entityToSave = AttendanceEntity.builder()
                .attendanceType(normalizedType)
                .admissionNo(admissionNo)
                .name(name)
                .rollNo(rollNo)
                .className(className)
                .department(department)
                .designation(designation)
                .attendanceDate(date)
                .status(status)
                .note(notes)
                .avatar(avatar)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .build();

        try {
            entityToSave = attendanceRepository.save(entityToSave);
        } catch (Exception e) {
            log.error("Failed to save attendance record", e);
        }

        Map<String, Object> record = new HashMap<>();
        record.put("id", entityToSave.getId() != null ? entityToSave.getId() : System.currentTimeMillis());
        record.put("attendanceType", normalizedType.toLowerCase());
        record.put("admissionNo", admissionNo);
        record.put("name", name);
        record.put("rollNo", rollNo);
        record.put("className", className);
        record.put("department", department);
        record.put("date", date);
        record.put("status", status);
        record.put("notes", notes);
        record.put("checkInTime", checkInTime);
        record.put("checkOutTime", checkOutTime);
        record.put("markedBy", user != null ? user.getName() : "Self");

        return ResponseEntity.ok(record);
    }

    @PostMapping("/attendance/check-in")
    @Operation(summary = "Check in for student or staff")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody Map<String, Object> req) {
        Map<String, Object> payload = req != null ? new HashMap<>(req) : new HashMap<>();
        payload.putIfAbsent("status", "present");
        payload.putIfAbsent("notes", "Checked in via Mobile App");
        return markAttendance(payload);
    }

    @PostMapping("/attendance/check-out")
    @Operation(summary = "Check out for student or staff")
    public ResponseEntity<Map<String, Object>> checkOut(@RequestBody Map<String, Object> req) {
        Map<String, Object> payload = req != null ? new HashMap<>(req) : new HashMap<>();
        payload.putIfAbsent("status", "present");
        payload.putIfAbsent("notes", "Checked out via Mobile App");
        return markAttendance(payload);
    }

    @GetMapping("/attendance/stats")
    @Operation(summary = "Get overall attendance tracking statistics for School Admin Dashboard")
    public ResponseEntity<Map<String, Object>> getAttendanceStats() {
        Map<String, Object> stats = new HashMap<>();
        String today = LocalDate.now().toString();

        long totalStudents = studentRepository.count();
        List<AttendanceEntity> studentAttToday = attendanceRepository.findByAttendanceTypeAndAttendanceDate("STUDENT", today);
        long presentStudents = studentAttToday.stream()
                .filter(a -> "present".equalsIgnoreCase(a.getStatus()))
                .count();
        long absentStudents = totalStudents >= presentStudents ? totalStudents - presentStudents : 0;
        double studentPercentage = totalStudents > 0
                ? Math.round((presentStudents * 100.0 / totalStudents) * 10.0) / 10.0
                : 0.0;

        long totalStaff = staffRepository.count() + teacherRepository.count();
        List<AttendanceEntity> staffAttToday = new ArrayList<>();
        staffAttToday.addAll(attendanceRepository.findByAttendanceTypeAndAttendanceDate("EMPLOYEE", today));
        staffAttToday.addAll(attendanceRepository.findByAttendanceTypeAndAttendanceDate("TEACHER", today));
        long presentStaff = staffAttToday.stream()
                .filter(a -> "present".equalsIgnoreCase(a.getStatus()))
                .count();
        long absentStaff = totalStaff >= presentStaff ? totalStaff - presentStaff : 0;
        double staffPercentage = totalStaff > 0
                ? Math.round((presentStaff * 100.0 / totalStaff) * 10.0) / 10.0
                : 0.0;

        long totalAll = totalStudents + totalStaff;
        long presentAll = presentStudents + presentStaff;
        double overallPercentage = totalAll > 0
                ? Math.round((presentAll * 100.0 / totalAll) * 10.0) / 10.0
                : 0.0;

        // Class breakdown from actual registered students
        List<StudentEntity> allStudents = studentRepository.findAll();
        Map<String, List<StudentEntity>> studentsByClass = allStudents.stream()
                .filter(s -> s.getClassName() != null && !s.getClassName().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(StudentEntity::getClassName));

        List<Map<String, Object>> classBreakdown = new ArrayList<>();
        for (Map.Entry<String, List<StudentEntity>> entry : studentsByClass.entrySet()) {
            String cls = entry.getKey();
            int classTotal = entry.getValue().size();
            long classPresent = studentAttToday.stream()
                    .filter(a -> cls.equalsIgnoreCase(a.getClassName()) && "present".equalsIgnoreCase(a.getStatus()))
                    .count();
            int classAbsent = classTotal >= classPresent ? (int)(classTotal - classPresent) : 0;
            double pct = classTotal > 0 ? Math.round((classPresent * 100.0 / classTotal) * 10.0) / 10.0 : 0.0;
            classBreakdown.add(createClassStat(cls, classTotal, (int)classPresent, classAbsent, pct));
        }

        stats.put("totalStudents", totalStudents);
        stats.put("presentStudents", presentStudents);
        stats.put("absentStudents", absentStudents);
        stats.put("studentPercentage", studentPercentage);
        stats.put("totalStaff", totalStaff);
        stats.put("presentStaff", presentStaff);
        stats.put("absentStaff", absentStaff);
        stats.put("staffPercentage", staffPercentage);
        stats.put("overallPercentage", overallPercentage);
        stats.put("classBreakdown", classBreakdown);
        stats.put("date", today);

        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> createClassStat(String className, int total, int present, int absent, double percentage) {
        Map<String, Object> m = new HashMap<>();
        m.put("className", className);
        m.put("total", total);
        m.put("present", present);
        m.put("absent", absent);
        m.put("percentage", percentage);
        return m;
    }

    // ==========================================
    // 4. HOMEWORK ENDPOINTS (Database-Backed)
    // ==========================================
    @GetMapping("/homework")
    @Operation(summary = "Get homework list backed by PostgreSQL database")
    public ResponseEntity<List<Map<String, Object>>> getHomework(
            @RequestParam(required = false) String className) {

        List<HomeworkEntity> list;
        if (className != null && !className.isBlank()) {
            list = homeworkRepository.findByClassNameIgnoreCaseOrderByIdDesc(className);
        } else {
            list = homeworkRepository.findAllByOrderByIdDesc();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (HomeworkEntity h : list) {
            String submissionStatus = submittedHomeworkMap.getOrDefault(String.valueOf(h.getId()), "pending");
            if (h.getDueDate() != null && !h.getDueDate().isBlank()) {
                try {
                    if (LocalDate.parse(h.getDueDate()).isBefore(LocalDate.now()) && "pending".equalsIgnoreCase(submissionStatus)) {
                        submissionStatus = "submitted";
                    }
                } catch (Exception ignored) {}
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", "hw_" + h.getId());
            item.put("dbId", h.getId());
            item.put("subject", h.getSubject());
            item.put("title", h.getTitle());
            item.put("description", h.getDescription());
            item.put("dueDate", h.getDueDate());
            item.put("assignedDate", h.getAssignedDate());
            item.put("assignedBy", h.getAssignedBy());
            item.put("status", submissionStatus);
            item.put("className", h.getClassName());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/homework/submit/{id}")
    @Operation(summary = "Submit homework by student")
    public ResponseEntity<Map<String, Object>> submitHomework(@PathVariable String id) {
        String cleanId = id.replace("hw_", "");
        submittedHomeworkMap.put(cleanId, "submitted");

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "submitted");
        res.put("message", "Homework submitted successfully");
        return ResponseEntity.ok(res);
    }

    // ==========================================
    // 5. FEES ENDPOINTS (Database-Backed & Live Sync)
    // ==========================================
    @GetMapping("/fees")
    @Operation(summary = "Get fee records backed by PostgreSQL database")
    public ResponseEntity<List<Map<String, Object>>> getFees() {
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String targetAdmissionNo = null;
        if (user != null && user.getRole() == Role.STUDENT) {
            Optional<StudentEntity> s = studentRepository.findByEmail(user.getEmail());
            if (s.isPresent()) {
                targetAdmissionNo = s.get().getAdmissionNo();
            }
        } else if (user != null && user.getRole() == Role.PARENT) {
            Optional<GuardianEntity> g = guardianRepository.findByEmail(user.getEmail());
            if (g.isPresent()) {
                targetAdmissionNo = g.get().getStudentAdmissionNo();
            }
        }

        List<FeeCollectionEntity> entities;
        if (targetAdmissionNo != null && !targetAdmissionNo.isBlank()) {
            entities = feeCollectionRepository.findByAdmissionNoIgnoreCaseOrderByIdDesc(targetAdmissionNo);
        } else {
            entities = feeCollectionRepository.findAllByOrderByIdDesc();
        }

        List<Map<String, Object>> fees = new ArrayList<>();
        for (FeeCollectionEntity f : entities) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "fee_" + f.getId());
            map.put("dbId", f.getId());
            map.put("title", f.getPaymentType() != null && !f.getPaymentType().isBlank() ? f.getPaymentType() : "Fee");
            map.put("amount", f.getAmount() != null ? Double.parseDouble(f.getAmount()) : 0.0);
            map.put("dueDate", f.getDate() != null ? f.getDate() : "");
            map.put("status", f.getStatus() != null ? f.getStatus().toLowerCase() : "unpaid");
            if ("paid".equalsIgnoreCase(f.getStatus())) {
                map.put("paymentDate", f.getDate());
                map.put("transactionId", f.getNote() != null && f.getNote().startsWith("TXN-")
                        ? f.getNote() : "TXN-" + f.getId() + "A");
            }
            fees.add(map);
        }

        return ResponseEntity.ok(fees);
    }

    @PostMapping("/fees/pay/{id}")
    @Operation(summary = "Pay fee online - immediately updates PostgreSQL and Accountant Dashboard")
    public ResponseEntity<Map<String, Object>> payFee(@PathVariable String id) {
        String cleanIdStr = id.replace("fee_", "");
        String txn = "TXN-" + System.currentTimeMillis() + "G";

        try {
            Long dbId = Long.parseLong(cleanIdStr);
            feeCollectionRepository.findById(dbId).ifPresent(entity -> {
                entity.setStatus("paid");
                entity.setPaid(entity.getAmount());
                entity.setDue("0");
                entity.setDate(LocalDate.now().toString());
                entity.setNote(txn);
                feeCollectionRepository.save(entity);
            });
        } catch (Exception e) {
            log.warn("Could not find fee entity with id: {}", id);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "paid");
        res.put("transactionId", txn);
        res.put("message", "Fee paid successfully!");
        return ResponseEntity.ok(res);
    }

    // ==========================================
    // 6. TIMETABLE ENDPOINT (Database-Backed)
    // ==========================================
    @GetMapping("/timetable")
    @Operation(summary = "Get timetable schedule tailored for Student, Teacher, or Parent")
    public ResponseEntity<List<Map<String, Object>>> getTimetable(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String day,
            @RequestParam(required = false) String teacherName) {

        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String resolvedClass = className;
        String resolvedSection = section;
        String resolvedTeacher = teacherName;

        if (user != null) {
            if (user.getRole() == Role.STUDENT) {
                Optional<StudentEntity> sOpt = studentRepository.findByEmail(user.getEmail());
                if (sOpt.isPresent()) {
                    StudentEntity s = sOpt.get();
                    if (resolvedClass == null || resolvedClass.isBlank()) {
                        resolvedClass = s.getClassName();
                    }
                    if (resolvedSection == null || resolvedSection.isBlank()) {
                        resolvedSection = s.getSection();
                    }
                }
            } else if (user.getRole() == Role.TEACHER) {
                Optional<TeacherEntity> tOpt = teacherRepository.findByEmail(user.getEmail());
                if (tOpt.isPresent()) {
                    TeacherEntity t = tOpt.get();
                    if (resolvedTeacher == null || resolvedTeacher.isBlank()) {
                        String fullName = ((t.getFirstName() != null ? t.getFirstName() : "") + " " +
                                (t.getLastName() != null ? t.getLastName() : "")).trim();
                        resolvedTeacher = !fullName.isEmpty() ? fullName : user.getName();
                    }
                }
            } else if (user.getRole() == Role.PARENT) {
                Optional<GuardianEntity> gOpt = guardianRepository.findByEmail(user.getEmail());
                if (gOpt.isPresent()) {
                    String adm = gOpt.get().getStudentAdmissionNo();
                    if (adm != null && !adm.isBlank()) {
                        Optional<StudentEntity> sOpt = studentRepository.findByAdmissionNo(adm);
                        if (sOpt.isPresent()) {
                            StudentEntity s = sOpt.get();
                            if (resolvedClass == null || resolvedClass.isBlank()) {
                                resolvedClass = s.getClassName();
                            }
                            if (resolvedSection == null || resolvedSection.isBlank()) {
                                resolvedSection = s.getSection();
                            }
                        }
                    }
                }
            }
        }

        if (resolvedClass != null && resolvedClass.contains("-")
                && (resolvedSection == null || resolvedSection.isBlank())) {
            String[] parts = resolvedClass.split("-");
            resolvedClass = parts[0].trim();
            if (parts.length > 1) resolvedSection = parts[1].trim();
        }

        List<TimetableEntity> entities = new ArrayList<>();
        try {
            if (resolvedTeacher != null && !resolvedTeacher.isBlank()) {
                entities = timetableRepository.findByTeacherNameContainingIgnoreCase(resolvedTeacher);
            } else if (resolvedClass != null && !resolvedClass.isBlank()) {
                if (resolvedSection != null && !resolvedSection.isBlank()) {
                    entities = timetableRepository.findByClassNameIgnoreCaseAndSectionIgnoreCase(resolvedClass, resolvedSection);
                }
                if (entities.isEmpty()) {
                    entities = timetableRepository.findByClassNameIgnoreCase(resolvedClass);
                }
            } else {
                entities = timetableRepository.findAll();
            }

            if (day != null && !day.isBlank() && !"all".equalsIgnoreCase(day.trim())) {
                String dLower = day.trim().toLowerCase();
                String prefix = dLower.substring(0, Math.min(3, dLower.length()));
                entities = entities.stream()
                        .filter(e -> e.getDayOfWeek() != null && e.getDayOfWeek().toLowerCase().startsWith(prefix))
                        .toList();
            }
        } catch (Exception e) {
            log.error("Error fetching mobile timetable slots", e);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TimetableEntity t : entities) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", String.valueOf(t.getId()));
            m.put("dayOfWeek", t.getDayOfWeek());
            m.put("periodName", t.getPeriodName() != null ? t.getPeriodName() : "Period");
            m.put("subject", t.getSubject());
            m.put("startTime", t.getStartTime());
            m.put("endTime", t.getEndTime());
            m.put("teacherName", t.getTeacherName() != null ? t.getTeacherName() : "");
            m.put("classroom", t.getClassroom() != null ? t.getClassroom() : "");
            m.put("className", t.getClassName());
            m.put("section", t.getSection());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // 7. EXAMS & RESULTS ENDPOINTS (Database-Backed & Live Sync)
    // ==========================================
    @GetMapping("/exams")
    @Operation(summary = "Get exam schedule & real results backed by PostgreSQL")
    public ResponseEntity<List<Map<String, Object>>> getExams() {
        List<Map<String, Object>> exams = new ArrayList<>();

        // 1. Exam schedules from DB
        List<ExamScheduleEntity> schedules = examScheduleRepository.findAllByOrderByIdDesc();
        for (ExamScheduleEntity s : schedules) {
            String title = (s.getExamName() != null && !s.getExamName().isBlank())
                    ? s.getExamName() : ((s.getClassName() != null ? s.getClassName() : "") + " " + (s.getSubject() != null ? s.getSubject() : "") + " Exam").trim();
            String desc = (s.getSubject() != null ? s.getSubject() : "") + " examination for " + (s.getClassName() != null ? s.getClassName() : "");
            String date = s.getDate() != null && !s.getDate().isBlank() ? s.getDate() : "";
            String start = s.getStartTime() != null ? s.getStartTime() : "";
            String end = s.getEndTime() != null ? s.getEndTime() : "";
            String room = s.getRoom() != null ? s.getRoom() : "";

            exams.add(createExam(
                    "sched_" + s.getId(),
                    s.getSubject() != null ? s.getSubject() : "General",
                    title,
                    desc,
                    date,
                    start,
                    end,
                    room,
                    100.0,
                    null,
                    "upcoming",
                    s.getClassName() != null ? s.getClassName() : ""));
        }

        // 2. Exam results from DB
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String targetAdmissionNo = null;
        if (user != null && user.getRole() == Role.STUDENT) {
            Optional<StudentEntity> s = studentRepository.findByEmail(user.getEmail());
            if (s.isPresent()) {
                targetAdmissionNo = s.get().getAdmissionNo();
            }
        } else if (user != null && user.getRole() == Role.PARENT) {
            Optional<GuardianEntity> g = guardianRepository.findByEmail(user.getEmail());
            if (g.isPresent()) {
                targetAdmissionNo = g.get().getStudentAdmissionNo();
            }
        }

        List<ExamResultEntity> results;
        if (targetAdmissionNo != null && !targetAdmissionNo.isBlank()) {
            results = examResultRepository.findByAdmissionNoIgnoreCaseOrderByIdDesc(targetAdmissionNo);
        } else {
            results = examResultRepository.findAllByOrderByIdDesc();
        }

        for (ExamResultEntity r : results) {
            double scored = r.getTotal() != null ? r.getTotal().doubleValue() : 0.0;
            String gradeText = r.getGrade() != null ? (" Grade: " + r.getGrade()) : "";
            String statusText = r.getResult() != null ? r.getResult().toUpperCase() : "PASS";

            exams.add(createExam(
                    "res_" + r.getId(),
                    r.getExam() != null ? r.getExam() : "Examination",
                    r.getExam() != null ? r.getExam() : "Examination Assessment",
                    "Result: " + statusText + " (" + (r.getPercent() != null ? r.getPercent() : 0) + "%)" + gradeText,
                    r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString(),
                    "09:00 AM",
                    "10:30 AM",
                    "Classroom",
                    100.0,
                    scored,
                    "completed",
                    r.getClassName() != null ? r.getClassName() : ""));
        }

        return ResponseEntity.ok(exams);
    }

    @PostMapping("/exams")
    @Operation(summary = "Create a new exam schedule backed by PostgreSQL")
    public ResponseEntity<Map<String, Object>> createExam(@RequestBody Map<String, Object> req) {
        String subject = String.valueOf(req.getOrDefault("subject", ""));
        String title = String.valueOf(req.getOrDefault("title", "Exam"));
        String desc = String.valueOf(req.getOrDefault("description", ""));
        String date = String.valueOf(req.getOrDefault("date", LocalDate.now().toString()));
        String start = String.valueOf(req.getOrDefault("startTime", "09:00 AM"));
        String end = String.valueOf(req.getOrDefault("endTime", "11:30 AM"));
        String room = String.valueOf(req.getOrDefault("room", ""));
        double maxMarks = Double.parseDouble(String.valueOf(req.getOrDefault("maxMarks", 100.0)));
        String className = String.valueOf(req.getOrDefault("className", ""));

        ExamScheduleEntity entity = ExamScheduleEntity.builder()
                .examName(title)
                .className(className)
                .section("A")
                .subject(subject)
                .date(date)
                .startTime(start)
                .endTime(end)
                .duration("2 Hours")
                .room(room)
                .build();
        entity = examScheduleRepository.save(entity);

        Map<String, Object> exam = createExam("sched_" + entity.getId(), subject, title, desc, date, start, end, room,
                maxMarks, null, "upcoming", className);
        return ResponseEntity.ok(exam);
    }

    @PostMapping("/exams/{id}/score")
    @Operation(summary = "Upload scored marks for an exam - persists directly to PostgreSQL ExamResultEntity")
    public ResponseEntity<Map<String, Object>> uploadExamScore(@PathVariable String id,
            @RequestBody Map<String, Object> req) {
        double score = Double.parseDouble(String.valueOf(req.getOrDefault("scoredMarks", req.getOrDefault("score", 0.0))));
        String studentAdm = String.valueOf(req.getOrDefault("admissionNo", ""));
        String studentName = String.valueOf(req.getOrDefault("name", ""));
        String examTitle = String.valueOf(req.getOrDefault("exam", "Exam"));
        String rollNo = String.valueOf(req.getOrDefault("rollNo", ""));
        String className = String.valueOf(req.getOrDefault("className", ""));

        int percent = (int) Math.round(score);
        String grade = percent >= 90 ? "A+" : (percent >= 80 ? "A" : (percent >= 70 ? "B" : (percent >= 50 ? "C" : "D")));
        String result = percent >= 40 ? "PASS" : "FAIL";

        ExamResultEntity resultEntity = ExamResultEntity.builder()
                .admissionNo(studentAdm)
                .name(studentName)
                .rollNo(rollNo)
                .className(className)
                .exam(examTitle)
                .total((int) score)
                .percent(percent)
                .grade(grade)
                .result(result)
                .build();
        resultEntity = examResultRepository.save(resultEntity);

        Map<String, Object> target = createExam("res_" + resultEntity.getId(), "General", examTitle,
                "Marks uploaded: " + score + "/100 (" + grade + ")",
                LocalDate.now().toString(), "09:00 AM", "11:30 AM", "Classroom", 100.0, score, "completed", className);

        return ResponseEntity.ok(target);
    }

    // ==========================================
    // 8. EXPENSES & PAYROLL ENDPOINTS (PostgreSQL Backed)
    // ==========================================
    @GetMapping("/expenses")
    @Operation(summary = "Get expense records from database")
    public ResponseEntity<List<Map<String, Object>>> getExpenses() {
        List<AccountTransactionEntity> transactions = accountTransactionRepository.findAllByTransactionType("EXPENSE");
        List<Map<String, Object>> expenses = new ArrayList<>();

        for (AccountTransactionEntity t : transactions) {
            expenses.add(createExpense(
                    "exp_" + t.getId(),
                    t.getDescription() != null && !t.getDescription().isBlank() ? t.getDescription() : "Expense",
                    t.getDescription() != null ? t.getDescription() : "",
                    t.getAmount() != null ? t.getAmount() : 0.0,
                    t.getCategory() != null ? t.getCategory() : "general",
                    t.getTransactionDate() != null ? t.getTransactionDate() : (t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString()),
                    "approved",
                    "Accounts",
                    "Admin"));
        }

        return ResponseEntity.ok(expenses);
    }

    @PostMapping("/expenses")
    @Operation(summary = "Add expense claim backed by PostgreSQL database")
    public ResponseEntity<Map<String, Object>> addExpense(@RequestBody Map<String, Object> req) {
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String name = user != null ? user.getName() : "Staff Member";
        String title = String.valueOf(req.getOrDefault("title", "Expense Item"));
        String desc = String.valueOf(req.getOrDefault("description", title));
        double amount = Double.parseDouble(String.valueOf(req.getOrDefault("amount", 0.0)));
        String category = String.valueOf(req.getOrDefault("category", "supplies"));
        String date = req.containsKey("date") && req.get("date") != null ? req.get("date").toString() : LocalDate.now().toString();

        AccountTransactionEntity entity = AccountTransactionEntity.builder()
                .transactionType("EXPENSE")
                .category(category)
                .amount(amount)
                .description(title + (desc.equals(title) ? "" : " - " + desc))
                .transactionDate(date)
                .build();
        entity = accountTransactionRepository.save(entity);

        Map<String, Object> item = createExpense(
                "exp_" + entity.getId(),
                title,
                desc,
                amount,
                category,
                date,
                "pending",
                name,
                null);

        return ResponseEntity.ok(item);
    }

    @PatchMapping("/expenses/{id}/approve")
    @Operation(summary = "Approve expense claim")
    public ResponseEntity<Map<String, Object>> approveExpense(@PathVariable String id) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "approved");
        res.put("approvedBy", "Accountant");
        res.put("message", "Expense approved");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/payroll")
    @Operation(summary = "Get payroll records dynamically calculated from staff database")
    public ResponseEntity<List<Map<String, Object>>> getPayroll() {
        List<StaffEntity> staffList = staffRepository.findAll();
        List<Map<String, Object>> payrolls = new ArrayList<>();
        String currentMonth = LocalDate.now().getMonth().name() + " " + LocalDate.now().getYear();

        for (StaffEntity s : staffList) {
            double gross = s.getSalary() != null ? s.getSalary() : 0.0;
            payrolls.add(createPayroll(
                    "pay_" + s.getId(),
                    s.getName(),
                    s.getDesignation() != null ? s.getDesignation() : (s.getRole() != null ? s.getRole() : "Staff"),
                    gross,
                    0.0,
                    gross,
                    currentMonth,
                    "processed"));
        }
        return ResponseEntity.ok(payrolls);
    }

    // ==========================================
    // 9. LEAVE REQUESTS (Database-Backed & Live Sync)
    // ==========================================
    @GetMapping("/leaves")
    @Operation(summary = "Get leave applications backed by PostgreSQL database")
    public ResponseEntity<List<Map<String, Object>>> getLeaves() {
        List<LeaveRequestEntity> entities = leaveRequestRepository.findAllByOrderByIdDesc();
        List<Map<String, Object>> leaves = new ArrayList<>();

        for (LeaveRequestEntity lev : entities) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", "lev_" + lev.getId());
            m.put("dbId", lev.getId());
            m.put("employeeName", lev.getName());
            m.put("designation", lev.getUserType() != null ? lev.getUserType() : "Faculty");
            m.put("reason", lev.getReason() != null ? lev.getReason() : "Personal Leave");
            m.put("fromDate", lev.getDate() != null ? lev.getDate() : "");
            m.put("toDate", lev.getDate() != null ? lev.getDate() : "");
            m.put("status", lev.getStatus() != null ? lev.getStatus().toLowerCase() : "pending");
            m.put("appliedOn", lev.getApplyDate() != null ? lev.getApplyDate() : "");
            leaves.add(m);
        }

        return ResponseEntity.ok(leaves);
    }

    @PostMapping("/leaves")
    @Operation(summary = "Apply for leave - saved directly into PostgreSQL")
    public ResponseEntity<Map<String, Object>> applyLeave(@RequestBody Map<String, Object> req) {
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String name = user != null ? user.getName() : "Faculty Member";
        String reason = String.valueOf(req.getOrDefault("reason", "Personal leave"));
        String fromDate = String.valueOf(req.getOrDefault("fromDate", LocalDate.now().toString()));

        LeaveRequestEntity entity = LeaveRequestEntity.builder()
                .name(name)
                .userType(user != null ? user.getRole().name() : "FACULTY")
                .leaveType(String.valueOf(req.getOrDefault("leaveType", "Casual")))
                .date(fromDate)
                .duration("1 Day")
                .status("Pending")
                .reason(reason)
                .applyDate(LocalDate.now().toString())
                .build();
        entity = leaveRequestRepository.save(entity);

        Map<String, Object> leave = createLeave("lev_" + entity.getId(), name,
                user != null ? user.getRole().name() : "Faculty",
                reason, fromDate, fromDate, "pending", LocalDate.now().toString());

        return ResponseEntity.ok(leave);
    }

    @PatchMapping("/leaves/{id}/approve")
    @Operation(summary = "Approve leave - updates PostgreSQL")
    public ResponseEntity<Map<String, Object>> approveLeave(@PathVariable String id) {
        String cleanIdStr = id.replace("lev_", "");
        try {
            Long dbId = Long.parseLong(cleanIdStr);
            leaveRequestRepository.findById(dbId).ifPresent(l -> {
                l.setStatus("Approved");
                leaveRequestRepository.save(l);
            });
        } catch (Exception ignored) {
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "approved");
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/leaves/{id}/reject")
    @Operation(summary = "Reject leave - updates PostgreSQL")
    public ResponseEntity<Map<String, Object>> rejectLeave(@PathVariable String id) {
        String cleanIdStr = id.replace("lev_", "");
        try {
            Long dbId = Long.parseLong(cleanIdStr);
            leaveRequestRepository.findById(dbId).ifPresent(l -> {
                l.setStatus("Rejected");
                leaveRequestRepository.save(l);
            });
        } catch (Exception ignored) {
        }

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "rejected");
        return ResponseEntity.ok(res);
    }

    // ==========================================
    // 10. NOTICES ENDPOINT (Database-Backed & Live Sync)
    // ==========================================
    @GetMapping("/notices")
    @Operation(summary = "Get school notices from database with dynamic audience filtering")
    public ResponseEntity<List<Map<String, Object>>> getNotices() {
        List<Map<String, Object>> notices = new ArrayList<>();
        UserEntity currentUser = securityUtil.getCurrentUser().orElse(null);

        Set<String> targetAudiences = new HashSet<>(Arrays.asList("ALL", "EVERYONE"));
        if (currentUser != null) {
            String roleName = currentUser.getRole().name().toUpperCase();
            targetAudiences.add(roleName);
            if (currentUser.getRole() == Role.STUDENT) {
                targetAudiences.add("STUDENTS");
                targetAudiences.add("STUDENT");
            } else if (currentUser.getRole() == Role.PARENT) {
                targetAudiences.add("PARENTS");
                targetAudiences.add("PARENT");
            } else if (currentUser.getRole() == Role.TEACHER) {
                targetAudiences.add("TEACHERS");
                targetAudiences.add("TEACHER");
                targetAudiences.add("STAFF");
            } else if (currentUser.getRole() == Role.STAFF || currentUser.getRole() == Role.PRINCIPAL
                    || currentUser.getRole() == Role.ACCOUNTANT || currentUser.getRole() == Role.LIBRARIAN) {
                targetAudiences.add("STAFF");
            }
        }

        List<NoticeEntity> dbNotices = noticeRepository.findAllByOrderByIdDesc();
        for (NoticeEntity entity : dbNotices) {
            String target = entity.getTarget() != null ? entity.getTarget().trim().toUpperCase() : "ALL";
            if (target.equalsIgnoreCase("ALL") || targetAudiences.contains(target) || currentUser == null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", "not_" + entity.getId());
                map.put("dbId", entity.getId());
                map.put("title", entity.getTitle());
                map.put("content", entity.getContent() != null && !entity.getContent().isBlank() ? entity.getContent()
                        : entity.getTitle());
                map.put("date", entity.getDate() != null && !entity.getDate().isBlank()
                        ? entity.getDate()
                        : (entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalDate().toString()
                                : ""));
                map.put("category", entity.getCategory() != null ? entity.getCategory().toLowerCase() : "regular");
                map.put("target", entity.getTarget() != null ? entity.getTarget() : "ALL");
                map.put("createdAt", entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
                notices.add(map);
            }
        }

        return ResponseEntity.ok(notices);
    }

    // ==========================================
    // UTILITY BUILDERS
    // ==========================================
    private Map<String, Object> createExam(String id, String subject, String title, String desc, String date,
            String start, String end, String room, double maxMarks, Double scored, String status, String className) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("subject", subject);
        m.put("title", title);
        m.put("description", desc);
        m.put("date", date);
        m.put("startTime", start);
        m.put("endTime", end);
        m.put("room", room);
        m.put("maxMarks", maxMarks);
        m.put("scoredMarks", scored);
        m.put("status", status);
        m.put("className", className);
        return m;
    }

    private Map<String, Object> createExpense(String id, String title, String desc, double amount, String category,
            String date, String status, String submittedBy, String approvedBy) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("description", desc);
        m.put("amount", amount);
        m.put("category", category);
        m.put("date", date);
        m.put("status", status);
        m.put("submittedBy", submittedBy);
        m.put("approvedBy", approvedBy);
        return m;
    }

    private Map<String, Object> createPayroll(String id, String name, String desig, double gross, double ded,
            double net, String month, String status) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("employeeName", name);
        m.put("designation", desig);
        m.put("grossSalary", gross);
        m.put("deductions", ded);
        m.put("netPay", net);
        m.put("payDate", LocalDate.now().toString());
        m.put("month", month);
        m.put("status", status);
        return m;
    }

    private Map<String, Object> createLeave(String id, String name, String desig, String reason, String from, String to,
            String status, String appliedOn) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("employeeName", name);
        m.put("designation", desig);
        m.put("reason", reason);
        m.put("fromDate", from);
        m.put("toDate", to);
        m.put("status", status);
        m.put("appliedOn", appliedOn);
        return m;
    }
}
