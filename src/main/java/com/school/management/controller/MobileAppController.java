package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.school.management.entity.AttendanceEntity;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@Tag(name = "Mobile App", description = "Endpoints serving Flutter Mobile App features")
public class MobileAppController {

    private final SecurityUtil securityUtil;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;
    private final AttendanceRepository attendanceRepository;

    // In-memory persistent stores for interactive mobile app features
    private static final Map<String, List<Map<String, Object>>> homeworkSubmissions = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> paidFees = new ConcurrentHashMap<>();
    private static final List<Map<String, Object>> customExpenses = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, Object>> customLeaves = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, Object>> customAttendance = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, Object>> customExams = Collections.synchronizedList(new ArrayList<>());

    @GetMapping("/profile/me")
    @Operation(summary = "Get current mobile user profile with role-specific details")
    public ResponseEntity<Map<String, Object>> getProfile() {
        UserEntity user = securityUtil.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId().toString());
        profile.put("email", user.getEmail());
        profile.put("name", user.getName());
        profile.put("role", user.getRole().name().toLowerCase());

        if (user.getRole() == Role.STUDENT) {
            studentRepository.findByEmail(user.getEmail()).ifPresentOrElse(s -> {
                profile.put("name", s.getName());
                profile.put("className", s.getClassName());
                profile.put("details", "Roll No: " + s.getRollNo() + " • " + s.getClassName());
                profile.put("avatarUrl", s.getStudentPhoto() != null ? s.getStudentPhoto() : "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150");
                profile.put("phone", s.getPhone());
                profile.put("fatherName", s.getFatherName());
            }, () -> {
                profile.put("className", "Class 10-A");
                profile.put("details", "Roll No: 24 • Class 10-A");
                profile.put("avatarUrl", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150");
            });
        } else if (user.getRole() == Role.PARENT) {
            guardianRepository.findByEmail(user.getEmail()).ifPresentOrElse(g -> {
                profile.put("name", g.getName());
                profile.put("className", "Parent of " + (g.getStudentAdmissionNo() != null ? g.getStudentAdmissionNo() : "Rohan Sharma"));
                profile.put("details", "Parent of Rohan Sharma (10-A)");
                profile.put("avatarUrl", g.getPhoto() != null ? g.getPhoto() : "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150");
                profile.put("phone", g.getPhone());
            }, () -> {
                profile.put("className", "Parent of Rohan");
                profile.put("details", "Parent of Rohan Sharma (10-A)");
                profile.put("avatarUrl", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150");
            });
        } else if (user.getRole() == Role.TEACHER) {
            teacherRepository.findByEmail(user.getEmail()).ifPresentOrElse(t -> {
                String fullName = (t.getFirstName() != null ? t.getFirstName() : "") + " " + (t.getLastName() != null ? t.getLastName() : "");
                profile.put("name", fullName.trim().isEmpty() ? user.getName() : fullName.trim());
                profile.put("className", t.getSubject() != null ? t.getSubject() + " Teacher" : "Mathematics Teacher");
                profile.put("details", (t.getDepartment() != null ? t.getDepartment() : "Science") + " Head Teacher");
                profile.put("avatarUrl", t.getAvatar() != null ? t.getAvatar() : "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150");
                profile.put("phone", t.getPhone());
            }, () -> {
                profile.put("className", "Maths Teacher");
                profile.put("details", "Mathematics Head Teacher");
                profile.put("avatarUrl", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150");
            });
        } else if (user.getRole() == Role.STAFF) {
            staffRepository.findByEmail(user.getEmail()).ifPresentOrElse(st -> {
                profile.put("name", st.getName());
                profile.put("className", st.getDesignation() != null ? st.getDesignation() : "Accountant");
                profile.put("details", (st.getStaffType() != null ? st.getStaffType() : "Accounts") + " & Finance Manager");
                profile.put("phone", st.getPhone());
            }, () -> {
                profile.put("className", "Accountant");
                profile.put("details", "Accounts & Finance Manager");
                profile.put("avatarUrl", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150");
            });
        }

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/attendance")
    @Operation(summary = "Get attendance records with optional role/date/className filters")
    public ResponseEntity<List<Map<String, Object>>> getAttendance(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String className) {
        
        List<Map<String, Object>> result = new ArrayList<>(customAttendance);
        
        // Also query AttendanceRepository database
        try {
            List<AttendanceEntity> dbEntities;
            if (type != null && date != null) {
                dbEntities = attendanceRepository.findByAttendanceTypeAndAttendanceDate(type, date);
            } else if (type != null) {
                dbEntities = attendanceRepository.findByAttendanceType(type);
            } else {
                dbEntities = attendanceRepository.findAll();
            }

            for (AttendanceEntity entity : dbEntities) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("id", entity.getId());
                rec.put("attendanceType", entity.getAttendanceType());
                rec.put("name", entity.getName());
                rec.put("rollNo", entity.getRollNo());
                rec.put("className", entity.getClassName());
                rec.put("department", entity.getDepartment());
                rec.put("designation", entity.getDesignation());
                rec.put("date", entity.getAttendanceDate() != null ? entity.getAttendanceDate() : LocalDate.now().toString());
                rec.put("status", entity.getStatus() != null ? entity.getStatus().toLowerCase() : "present");
                rec.put("notes", entity.getNote());
                rec.put("checkInTime", entity.getCheckInTime());
                rec.put("checkOutTime", entity.getCheckOutTime());
                rec.put("avatar", entity.getAvatar());
                result.add(0, rec);
            }
        } catch (Exception ignored) {}

        // If no custom or DB records exist yet, generate standard month records for current user
        if (result.isEmpty()) {
            LocalDate today = LocalDate.now();
            int daysInMonth = today.lengthOfMonth();
            int maxDay = today.getDayOfMonth();

            for (int day = 1; day <= daysInMonth; day++) {
                if (day > maxDay) continue;
                LocalDate d = LocalDate.of(today.getYear(), today.getMonth(), day);
                Map<String, Object> rec = new HashMap<>();
                rec.put("date", d.toString());
                rec.put("attendanceType", type != null ? type : "student");
                rec.put("name", "Student / User");

                if (d.getDayOfWeek().getValue() == 7) {
                    rec.put("status", "holiday");
                    rec.put("notes", "Sunday");
                } else if (day == 15) {
                    rec.put("status", "holiday");
                    rec.put("notes", "Mid Term Break");
                } else if (day == 8 || day == 22) {
                    rec.put("status", "absent");
                    rec.put("notes", "Medical Leave");
                } else {
                    rec.put("status", "present");
                }
                result.add(rec);
            }
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/attendance/mark")
    @Operation(summary = "Mark attendance for Student, Teacher, or Staff")
    public ResponseEntity<Map<String, Object>> markAttendance(@RequestBody Map<String, Object> req) {
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String name = req.containsKey("name") && req.get("name") != null && !req.get("name").toString().isEmpty()
                ? req.get("name").toString()
                : (user != null ? user.getName() : "School Member");

        String type = req.getOrDefault("attendanceType", req.getOrDefault("type", "student")).toString().toLowerCase();
        String date = req.getOrDefault("date", req.getOrDefault("attendanceDate", LocalDate.now().toString())).toString();
        String status = req.getOrDefault("status", "present").toString().toLowerCase();
        String notes = req.getOrDefault("notes", req.getOrDefault("note", "Marked via Mobile App")).toString();
        String className = req.getOrDefault("className", "Class 10-A").toString();
        String department = req.getOrDefault("department", "General").toString();
        String rollNo = req.getOrDefault("rollNo", req.getOrDefault("admissionNo", "24")).toString();

        String checkInTime = req.containsKey("checkInTime") && req.get("checkInTime") != null ? req.get("checkInTime").toString() : null;
        String checkOutTime = req.containsKey("checkOutTime") && req.get("checkOutTime") != null ? req.get("checkOutTime").toString() : null;

        Map<String, Object> record = new HashMap<>();
        record.put("id", System.currentTimeMillis());
        record.put("attendanceType", type);
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

        // Save to in-memory list
        customAttendance.add(0, record);

        // Save to DB AttendanceEntity
        try {
            AttendanceEntity entity = AttendanceEntity.builder()
                    .attendanceType(type)
                    .name(name)
                    .rollNo(rollNo)
                    .className(className)
                    .department(department)
                    .attendanceDate(date)
                    .status(status)
                    .note(notes)
                    .checkInTime(checkInTime)
                    .checkOutTime(checkOutTime)
                    .build();
            attendanceRepository.save(entity);
        } catch (Exception ignored) {}

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
        
        long totalStudents = 450;
        long presentStudents = 418 + customAttendance.stream().filter(a -> "student".equalsIgnoreCase(String.valueOf(a.get("attendanceType"))) && "present".equalsIgnoreCase(String.valueOf(a.get("status")))).count();
        long absentStudents = totalStudents - presentStudents;
        double studentPercentage = Math.round((presentStudents * 100.0 / totalStudents) * 10.0) / 10.0;

        long totalStaff = 45;
        long presentStaff = 42 + customAttendance.stream().filter(a -> !"student".equalsIgnoreCase(String.valueOf(a.get("attendanceType"))) && "present".equalsIgnoreCase(String.valueOf(a.get("status")))).count();
        long absentStaff = totalStaff - presentStaff;
        double staffPercentage = Math.round((presentStaff * 100.0 / totalStaff) * 10.0) / 10.0;

        double overallPercentage = Math.round(((presentStudents + presentStaff) * 100.0 / (totalStudents + totalStaff)) * 10.0) / 10.0;

        List<Map<String, Object>> classBreakdown = new ArrayList<>();
        classBreakdown.add(createClassStat("Class 10-A", 40, 38, 2, 95.0));
        classBreakdown.add(createClassStat("Class 10-B", 42, 39, 3, 92.8));
        classBreakdown.add(createClassStat("Class 9-A", 38, 36, 2, 94.7));
        classBreakdown.add(createClassStat("Class 9-B", 45, 41, 4, 91.1));
        classBreakdown.add(createClassStat("Class 8-A", 35, 34, 1, 97.1));

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
        stats.put("date", LocalDate.now().toString());

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

    @GetMapping("/homework")
    @Operation(summary = "Get homework list")
    public ResponseEntity<List<Map<String, Object>>> getHomework() {
        List<Map<String, Object>> items = new ArrayList<>();
        
        items.add(createHomework("hw_1", "Mathematics", "Quadratic Equations",
                "Complete questions 1 to 10 from exercises 4.2 in the classroom textbook.",
                LocalDate.now().plusDays(2).toString(), "pending", "Class 10-A"));
        items.add(createHomework("hw_2", "Science", "Solar System Project",
                "Build a three-dimensional model of the solar system using colored clay or standard cardboard.",
                LocalDate.now().plusDays(4).toString(), "pending", "Class 10-A"));
        items.add(createHomework("hw_3", "English", "Persuasive Essay Writing",
                "Write a 500-word essay on: The Crucial Importance of Outdoor Sports.",
                LocalDate.now().plusDays(6).toString(), "pending", "Class 10-A"));
        items.add(createHomework("hw_4", "History", "French Revolution Timeline",
                "Create a chronological timeline outlining major milestones of the French Revolution between 1789 and 1799.",
                LocalDate.now().minusDays(1).toString(), "submitted", "Class 10-A"));

        return ResponseEntity.ok(items);
    }

    @PostMapping("/homework/submit/{id}")
    @Operation(summary = "Submit homework by student")
    public ResponseEntity<Map<String, Object>> submitHomework(@PathVariable String id) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "submitted");
        res.put("message", "Homework submitted successfully");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/fees")
    @Operation(summary = "Get fee records")
    public ResponseEntity<List<Map<String, Object>>> getFees() {
        List<Map<String, Object>> fees = new ArrayList<>();

        Map<String, Object> f1 = new HashMap<>();
        f1.put("id", "fee_1");
        f1.put("title", "Term 1 Tuition Fees");
        f1.put("amount", 12450.0);
        f1.put("dueDate", LocalDate.now().plusDays(5).toString());
        if (paidFees.containsKey("fee_1")) {
            f1.put("status", "paid");
            f1.put("paymentDate", paidFees.get("fee_1").get("paymentDate"));
            f1.put("transactionId", paidFees.get("fee_1").get("transactionId"));
        } else {
            f1.put("status", "unpaid");
        }
        fees.add(f1);

        Map<String, Object> f2 = new HashMap<>();
        f2.put("id", "fee_2");
        f2.put("title", "Transport Fees (May)");
        f2.put("amount", 3450.0);
        f2.put("dueDate", LocalDate.now().minusDays(8).toString());
        f2.put("status", "paid");
        f2.put("paymentDate", LocalDate.now().minusDays(10).toString());
        f2.put("transactionId", "TXN-982348271A");
        fees.add(f2);

        Map<String, Object> f3 = new HashMap<>();
        f3.put("id", "fee_3");
        f3.put("title", "Examination Fees");
        f3.put("amount", 1200.0);
        f3.put("dueDate", LocalDate.now().minusDays(20).toString());
        f3.put("status", "paid");
        f3.put("paymentDate", LocalDate.now().minusDays(22).toString());
        f3.put("transactionId", "TXN-102934812B");
        fees.add(f3);

        return ResponseEntity.ok(fees);
    }

    @PostMapping("/fees/pay/{id}")
    @Operation(summary = "Pay fee online")
    public ResponseEntity<Map<String, Object>> payFee(@PathVariable String id) {
        String txn = "TXN-" + System.currentTimeMillis() + "G";
        Map<String, Object> info = new HashMap<>();
        info.put("paymentDate", LocalDate.now().toString());
        info.put("transactionId", txn);
        paidFees.put(id, info);

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "paid");
        res.put("transactionId", txn);
        res.put("message", "Fee paid successfully!");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/timetable")
    @Operation(summary = "Get timetable schedule")
    public ResponseEntity<List<Map<String, Object>>> getTimetable() {
        List<Map<String, Object>> slots = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            slots.add(createSlot(day + "_1", day, "Mathematics", "09:00 AM", "09:45 AM", "Ms. Priya", "Room 12"));
            slots.add(createSlot(day + "_2", day, "English Literature", "09:45 AM", "10:30 AM", "Mr. Sharma", "Room 12"));
            slots.add(createSlot(day + "_3", day, "Science Theory", "10:45 AM", "11:30 AM", "Dr. Verma", "Science Lab"));
            slots.add(createSlot(day + "_4", day, "Computer Coding", "11:30 AM", "12:15 PM", "Ms. Kulkarni", "Computer Lab"));
        }
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/exams")
    @Operation(summary = "Get exam schedule & results")
    public ResponseEntity<List<Map<String, Object>>> getExams() {
        List<Map<String, Object>> exams = new ArrayList<>(customExams);

        exams.add(createExam("exam_1", "Mathematics", "Term 1 Midterm Examination",
                "Algebra, Geometry & Trigonometry chapters from Term 1 syllabus.",
                LocalDate.now().plusDays(5).toString(), "09:00 AM", "11:30 AM", "Room 12", 100.0, null, "upcoming", "Class 10-A"));

        exams.add(createExam("exam_2", "Science", "Term 1 Science Assessment",
                "Physics, Chemistry & Biology Term 1 topics.",
                LocalDate.now().plusDays(8).toString(), "09:00 AM", "11:30 AM", "Room 15", 100.0, null, "upcoming", "Class 10-A"));

        exams.add(createExam("exam_5", "Mathematics", "Weekly Math Quiz",
                "Series of weekly math quizzes completed throughout the term.",
                LocalDate.now().minusDays(4).toString(), "10:00 AM", "10:45 AM", "Room 12", 25.0, 24.0, "completed", "Class 10-A"));

        exams.add(createExam("exam_6", "Science", "Weekly Science Test",
                "Weekly assessment covering physics and chemistry concepts.",
                LocalDate.now().minusDays(10).toString(), "09:00 AM", "10:00 AM", "Room 15", 30.0, 27.0, "completed", "Class 10-A"));

        return ResponseEntity.ok(exams);
    }

    @PostMapping("/exams")
    @Operation(summary = "Create a new exam schedule by Teacher or Admin")
    public ResponseEntity<Map<String, Object>> createExam(@RequestBody Map<String, Object> req) {
        String id = "exam_" + System.currentTimeMillis();
        String subject = String.valueOf(req.getOrDefault("subject", "Mathematics"));
        String title = String.valueOf(req.getOrDefault("title", "Term Assessment"));
        String desc = String.valueOf(req.getOrDefault("description", "Comprehensive term examination"));
        String date = String.valueOf(req.getOrDefault("date", LocalDate.now().plusDays(7).toString()));
        String start = String.valueOf(req.getOrDefault("startTime", "09:00 AM"));
        String end = String.valueOf(req.getOrDefault("endTime", "11:30 AM"));
        String room = String.valueOf(req.getOrDefault("room", "Room 12"));
        double maxMarks = Double.parseDouble(String.valueOf(req.getOrDefault("maxMarks", 100.0)));
        String className = String.valueOf(req.getOrDefault("className", "Class 10-A"));

        Map<String, Object> exam = createExam(id, subject, title, desc, date, start, end, room, maxMarks, null, "upcoming", className);
        customExams.add(0, exam);
        return ResponseEntity.ok(exam);
    }

    @PostMapping("/exams/{id}/score")
    @Operation(summary = "Upload scored marks for an exam by Teacher or Admin")
    public ResponseEntity<Map<String, Object>> uploadExamScore(@PathVariable String id, @RequestBody Map<String, Object> req) {
        double score = Double.parseDouble(String.valueOf(req.getOrDefault("scoredMarks", req.getOrDefault("score", 0.0))));

        Map<String, Object> target = null;
        for (Map<String, Object> ex : customExams) {
            if (id.equals(String.valueOf(ex.get("id")))) {
                target = ex;
                break;
            }
        }

        if (target != null) {
            target.put("scoredMarks", score);
            target.put("status", "completed");
        } else {
            target = createExam(id, "Mathematics", "Term 1 Midterm Examination",
                    "Algebra, Geometry & Trigonometry chapters.",
                    LocalDate.now().minusDays(1).toString(), "09:00 AM", "11:30 AM", "Room 12", 100.0, score, "completed", "Class 10-A");
            customExams.add(0, target);
        }

        return ResponseEntity.ok(target);
    }

    @GetMapping("/expenses")
    @Operation(summary = "Get expense claims for staff")
    public ResponseEntity<List<Map<String, Object>>> getExpenses() {
        List<Map<String, Object>> expenses = new ArrayList<>(customExpenses);

        expenses.add(createExpense("exp_1", "Library Books Purchase", "Purchase of reference books", 5400.0, "supplies",
                LocalDate.now().minusDays(2).toString(), "pending", "Ms. Priya (Maths Teacher)", null));

        expenses.add(createExpense("exp_2", "Bus Fuel Refill - Route 4", "Weekly diesel refill for school bus", 3200.0, "transport",
                LocalDate.now().minusDays(5).toString(), "pending", "Rajesh Kumar (Driver)", null));

        expenses.add(createExpense("exp_3", "Classroom Chair Repairs", "Repair of 12 wooden chairs in Class 10-A", 2150.0, "maintenance",
                LocalDate.now().minusDays(10).toString(), "approved", "Mr. Anthony (History Teacher)", "Rajesh Kumar (Accountant)"));

        expenses.add(createExpense("exp_4", "Electricity Bill", "Monthly electricity bill payment", 12400.0, "utilities",
                LocalDate.now().minusDays(15).toString(), "approved", "Admin Office", "Rajesh Kumar (Accountant)"));

        return ResponseEntity.ok(expenses);
    }

    @PostMapping("/expenses")
    @Operation(summary = "Add expense claim")
    public ResponseEntity<Map<String, Object>> addExpense(@RequestBody Map<String, Object> req) {
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String name = user != null ? user.getName() : "Staff Member";
        String id = "exp_" + System.currentTimeMillis();

        Map<String, Object> item = createExpense(id,
                String.valueOf(req.getOrDefault("title", "Expense Item")),
                String.valueOf(req.getOrDefault("description", "Claimed expense")),
                Double.parseDouble(String.valueOf(req.getOrDefault("amount", 1000))),
                String.valueOf(req.getOrDefault("category", "supplies")),
                LocalDate.now().toString(), "pending", name, null);

        customExpenses.add(0, item);
        return ResponseEntity.ok(item);
    }

    @PatchMapping("/expenses/{id}/approve")
    @Operation(summary = "Approve expense claim")
    public ResponseEntity<Map<String, Object>> approveExpense(@PathVariable String id) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "approved");
        res.put("approvedBy", "Rajesh Kumar (Accountant)");
        res.put("message", "Expense approved");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/payroll")
    @Operation(summary = "Get payroll records for staff")
    public ResponseEntity<List<Map<String, Object>>> getPayroll() {
        List<Map<String, Object>> payrolls = new ArrayList<>();
        payrolls.add(createPayroll("pay_1", "Rajesh Kumar", "Accountant", 48500.0, 6200.0, 42300.0, "June 2026", "processed"));
        payrolls.add(createPayroll("pay_2", "Ms. Priya Sharma", "Maths Teacher", 52000.0, 7800.0, 44200.0, "June 2026", "processed"));
        payrolls.add(createPayroll("pay_3", "Mr. Anthony D", "History Teacher", 49000.0, 7100.0, 41900.0, "June 2026", "processed"));
        payrolls.add(createPayroll("pay_4", "Coach Rawat", "Physical Education", 38000.0, 5400.0, 32600.0, "June 2026", "processed"));
        return ResponseEntity.ok(payrolls);
    }

    @GetMapping("/leaves")
    @Operation(summary = "Get leave applications")
    public ResponseEntity<List<Map<String, Object>>> getLeaves() {
        List<Map<String, Object>> leaves = new ArrayList<>(customLeaves);

        leaves.add(createLeave("lev_1", "Ms. Priya Sharma", "Maths Teacher",
                "Medical appointment - scheduled follow-up", LocalDate.now().plusDays(2).toString(),
                LocalDate.now().plusDays(2).toString(), "pending", LocalDate.now().minusDays(1).toString()));

        leaves.add(createLeave("lev_2", "Mr. Anthony D", "History Teacher",
                "Family function - cousin's wedding", LocalDate.now().plusDays(5).toString(),
                LocalDate.now().plusDays(6).toString(), "pending", LocalDate.now().minusDays(2).toString()));

        leaves.add(createLeave("lev_3", "Coach Rawat", "Physical Education",
                "Personal leave - urgent personal work", LocalDate.now().minusDays(5).toString(),
                LocalDate.now().minusDays(5).toString(), "approved", LocalDate.now().minusDays(7).toString()));

        return ResponseEntity.ok(leaves);
    }

    @PostMapping("/leaves")
    @Operation(summary = "Apply for leave")
    public ResponseEntity<Map<String, Object>> applyLeave(@RequestBody Map<String, Object> req) {
        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String name = user != null ? user.getName() : "Employee";
        String id = "lev_" + System.currentTimeMillis();

        Map<String, Object> leave = createLeave(id, name, "Faculty",
                String.valueOf(req.getOrDefault("reason", "Personal leave")),
                String.valueOf(req.getOrDefault("fromDate", LocalDate.now().toString())),
                String.valueOf(req.getOrDefault("toDate", LocalDate.now().toString())),
                "pending", LocalDate.now().toString());

        customLeaves.add(0, leave);
        return ResponseEntity.ok(leave);
    }

    @PatchMapping("/leaves/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLeave(@PathVariable String id) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "approved");
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/leaves/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectLeave(@PathVariable String id) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "rejected");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/notices")
    @Operation(summary = "Get school notices")
    public ResponseEntity<List<Map<String, Object>>> getNotices() {
        List<Map<String, Object>> notices = new ArrayList<>();

        Map<String, Object> n1 = new HashMap<>();
        n1.put("id", "not_1");
        n1.put("title", "Summer Vacation & Reopening Schedule");
        n1.put("content", "The school will remain closed for summer vacation from 1st June to 14th June. Regular classes commence from Monday, 15th June 2026.");
        n1.put("date", LocalDate.now().minusDays(1).toString());
        n1.put("category", "urgent");
        notices.add(n1);

        Map<String, Object> n2 = new HashMap<>();
        n2.put("id", "not_2");
        n2.put("title", "Parent Teacher Meeting (PTM) Details");
        n2.put("content", "A comprehensive Parent Teacher Meeting is scheduled for Monday, 25th May 2026, in the main assembly hall.");
        n2.put("date", LocalDate.now().minusDays(4).toString());
        n2.put("category", "regular");
        notices.add(n2);

        Map<String, Object> n3 = new HashMap<>();
        n3.put("id", "not_3");
        n3.put("title", "Annual Sports Day Registrations Open");
        n3.put("content", "Registrations are now open for the Annual Sports Day events. Students can register for running, relay race, and swimming.");
        n3.put("date", LocalDate.now().minusDays(6).toString());
        n3.put("category", "informational");
        notices.add(n3);

        return ResponseEntity.ok(notices);
    }

    // Utility helper builders
    private Map<String, Object> createHomework(String id, String subject, String title, String desc, String dueDate, String status, String className) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("subject", subject);
        m.put("title", title);
        m.put("description", desc);
        m.put("dueDate", dueDate);
        m.put("status", status);
        m.put("className", className);
        return m;
    }

    private Map<String, Object> createSlot(String id, String day, String subject, String start, String end, String teacher, String room) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("dayOfWeek", day);
        m.put("subject", subject);
        m.put("startTime", start);
        m.put("endTime", end);
        m.put("teacherName", teacher);
        m.put("classroom", room);
        return m;
    }

    private Map<String, Object> createExam(String id, String subject, String title, String desc, String date, String start, String end, String room, double maxMarks, Double scored, String status, String className) {
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

    private Map<String, Object> createExpense(String id, String title, String desc, double amount, String category, String date, String status, String submittedBy, String approvedBy) {
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

    private Map<String, Object> createPayroll(String id, String name, String desig, double gross, double ded, double net, String month, String status) {
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

    private Map<String, Object> createLeave(String id, String name, String desig, String reason, String from, String to, String status, String appliedOn) {
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
