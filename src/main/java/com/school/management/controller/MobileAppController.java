package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.dto.HolidayResponse;
import com.school.management.entity.*;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.service.HolidayService;
import com.school.management.service.NoticeService;
import com.school.management.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mobile App", description = "Enterprise endpoints serving Flutter Mobile App features backed by PostgreSQL")
public class MobileAppController {

    private final SecurityUtil securityUtil;
    private final com.school.management.service.PortalNotificationService portalNotifications;
    private final UserRepository portalUsers;
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
    private final SubjectRepository subjectRepository;
    private final BannerRepository bannerRepository;
    private final GalleryAlbumRepository galleryAlbumRepository;
    private final com.school.management.service.CurriculumService curriculumService;
    private final com.school.management.service.StudentTeachersService studentTeachersService;

    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final SubjectChapterRepository subjectChapterRepository;
    private final SubjectTopicRepository subjectTopicRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;

    private UserEntity assignmentManager() {
        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (!Set.of(Role.TEACHER, Role.ADMIN, Role.PRINCIPAL, Role.MASTER_ADMIN, Role.SUPER_ADMIN).contains(user.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher access required");
        return user;
    }

    private void requireAssignedClass(String className, String section) {
        UserEntity user = assignmentManager();
        if (user.getRole() != Role.TEACHER) return;
        TeacherEntity teacher = teacherRepository.findByEmailIgnoreCase(user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not linked"));
        boolean assigned = teacher.getAssignedClasses().stream().anyMatch(c ->
                c.getName().equalsIgnoreCase(className) &&
                (c.getSection() == null || c.getSection().isBlank() || c.getSection().equalsIgnoreCase(section)));
        if (!assigned && teacher.getAssignedClass() != null) {
            assigned = Arrays.stream(teacher.getAssignedClass().split(",")).anyMatch(c ->
                    c.trim().equalsIgnoreCase(className) || c.trim().equalsIgnoreCase(className + " - " + section));
        }
        if (!assigned) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Class/section is not assigned to this teacher");
    }

    private HomeworkEntity managedAssignment(String id) {
        Long key;
        try { key = Long.valueOf(id.replace("hw_", "")); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assignment ID"); }
        HomeworkEntity hw = homeworkRepository.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        requireAssignedClass(hw.getClassName(), hw.getSection());
        UserEntity user = assignmentManager();
        if (user.getRole() == Role.TEACHER) {
            TeacherEntity teacher = teacherRepository.findByEmailIgnoreCase(user.getEmail()).orElseThrow();
            if (!Objects.equals(hw.getTeacherId(), teacher.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assignment belongs to another teacher");
        }
        return hw;
    }

    private boolean assignedTo(HomeworkEntity hw, StudentEntity student) {
        if (hw.getTargetStudentId() != null) return hw.getTargetStudentId().equals(student.getId());
        return hw.getClassName() != null && hw.getClassName().equalsIgnoreCase(student.getClassName()) &&
                (hw.getSection() == null || hw.getSection().isBlank() || hw.getSection().equalsIgnoreCase(student.getSection()));
    }

    private StudentEntity assignmentStudent(String admissionNo) {
        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (user.getRole() == Role.STUDENT) {
            StudentEntity student = studentRepository.findByEmailIgnoreCase(user.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not linked"));
            if (admissionNo != null && !admissionNo.isBlank() && !admissionNo.trim().equalsIgnoreCase(student.getAdmissionNo()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view another student");
            return student;
        }
        if (user.getRole() != Role.PARENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student or parent access required");
        Map<Long, StudentEntity> children = new LinkedHashMap<>();
        studentRepository.findByGuardianEmailIgnoreCase(user.getEmail()).forEach(child -> children.put(child.getId(), child));
        guardianRepository.findByEmailIgnoreCase(user.getEmail()).ifPresent(parent -> {
            if (parent.getStudentAdmissionNo() != null)
                Arrays.stream(parent.getStudentAdmissionNo().split(",")).forEach(admission ->
                        studentRepository.findByAdmissionNoIgnoreCase(admission.trim()).ifPresent(child -> children.put(child.getId(), child)));
        });
        if (admissionNo == null || admissionNo.isBlank()) {
            if (children.size() == 1) return children.values().iterator().next();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a linked child");
        }
        return children.values().stream().filter(child -> admissionNo.trim().equalsIgnoreCase(child.getAdmissionNo())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Child is not linked to your account"));
    }

    // ==========================================
    // 0. COMPOSITE HOME DASHBOARD ENDPOINT
    // ==========================================
    @GetMapping("/dashboard/home")
    @Operation(summary = "Unified Composite Home Dashboard endpoint for Student and Parent")
    public ResponseEntity<Map<String, Object>> getHomeDashboard(
            @RequestParam(required = false) String admissionNo) {

        UserEntity currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        Map<String, Object> response = new HashMap<>();

        boolean isParentViewingChild = (currentUser.getRole() == Role.PARENT && admissionNo != null && !admissionNo.isBlank());
        boolean isStudent = (currentUser.getRole() == Role.STUDENT);

        if (isStudent || isParentViewingChild || currentUser.getRole() == Role.STAFF || currentUser.getRole() == Role.TEACHER) {
            // ================= STUDENT HOME VIEW =================
            StudentEntity student = null;
            if (admissionNo != null && !admissionNo.isBlank()) {
                student = studentRepository.findByAdmissionNoIgnoreCase(admissionNo.trim()).orElse(null);
            }
            if (student == null && isStudent) {
                student = studentRepository.findByEmailIgnoreCase(currentUser.getEmail()).orElse(null);
            }
            if (student == null && (isStudent || isParentViewingChild)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not linked");
            }
            if (isStudent && !currentUser.getEmail().equalsIgnoreCase(student.getEmail())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view another student");
            }
            if (isParentViewingChild) {
                String selectedAdmission = student.getAdmissionNo();
                boolean linked = studentRepository.findByGuardianEmailIgnoreCase(currentUser.getEmail()).stream()
                        .anyMatch(child -> selectedAdmission.equalsIgnoreCase(child.getAdmissionNo()));
                GuardianEntity parent = guardianRepository.findByEmailIgnoreCase(currentUser.getEmail()).orElse(null);
                if (parent != null && parent.getStudentAdmissionNo() != null) {
                    linked = linked || Arrays.stream(parent.getStudentAdmissionNo().split(","))
                            .anyMatch(admission -> selectedAdmission.equalsIgnoreCase(admission.trim()));
                }
                if (!linked) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Child is not linked to your account");
            }

            String studentName = student != null && student.getName() != null ? student.getName() : currentUser.getName();
            String studentClass = student != null && student.getClassName() != null ? student.getClassName() : "";
            if (!studentClass.toLowerCase().startsWith("class")) {
                studentClass = "Class : " + studentClass;
            }
            String rollNo = student != null && student.getRollNo() != null ? student.getRollNo() : "";
            String avatarUrl = student != null && student.getStudentPhoto() != null && !student.getStudentPhoto().isBlank()
                    ? student.getStudentPhoto()
                    : "";

            Map<String, Object> profile = new HashMap<>();
            profile.put("name", studentName);
            profile.put("className", studentClass);
            profile.put("rollNo", rollNo);
            profile.put("admissionNo", student != null ? student.getAdmissionNo() : "");
            profile.put("avatarUrl", avatarUrl);
            profile.put("headerSubtitle", studentClass + " | Roll No : " + rollNo);
            if (currentUser.getRole() == Role.TEACHER || currentUser.getRole() == Role.STAFF) {
                profile.put("name", currentUser.getName());
                profile.put("email", currentUser.getEmail());
                profile.put("className", "");
                profile.put("rollNo", "");
                profile.put("admissionNo", "");
                profile.put("avatarUrl", "");
                profile.put("headerSubtitle", currentUser.getRole().name());
                teacherRepository.findByEmailIgnoreCase(currentUser.getEmail()).ifPresent(teacher -> {
                    profile.put("avatarUrl", Objects.toString(teacher.getAvatar(), ""));
                    profile.put("headerSubtitle", Objects.toString(teacher.getSubject(), ""));
                });
            }


            // 1. Dynamic Banners from Database (Managed via Dashboard)
            List<Map<String, Object>> banners = new ArrayList<>();
            List<BannerEntity> dbBanners = bannerRepository.findByStatusIgnoreCaseOrderByDisplayOrderAsc("Active");
            for (BannerEntity b : dbBanners) {
                Map<String, Object> bm = new HashMap<>();
                bm.put("id", b.getId());
                bm.put("title", b.getTitle());
                bm.put("subtitle", b.getSubtitle() != null ? b.getSubtitle() : "");
                bm.put("badge", b.getBadge() != null ? b.getBadge() : "");
                bm.put("buttonText", b.getButtonText() != null ? b.getButtonText() : "Learn More");
                bm.put("websiteUrl", b.getWebsiteUrl() != null ? b.getWebsiteUrl() : "");
                bm.put("imageUrl", b.getImageUrl() != null ? b.getImageUrl() : "");
                bm.put("bgColorHex", b.getBgColorHex() != null ? b.getBgColorHex() : "#EAB308");
                banners.add(bm);
            }

            // 2. Subjects belong to the student's enrolled class.
            List<Map<String, Object>> subjects = new ArrayList<>();
            List<SubjectEntity> studentSubjects = new ArrayList<>();
            if (student != null) {
                studentSubjects = curriculumService.getStudentSubjects(student);
            }
            for (int i = 0; i < studentSubjects.size(); i++) {
                subjects.add(mapSubjectEntityToDashboardMap(studentSubjects.get(i), i));
            }

            // 3. Dynamic Upcoming Events from Database
            List<Map<String, Object>> events = new ArrayList<>();
            List<HolidayEntity> dbHolidays = holidayRepository.findAll();
            for (int i = 0; i < Math.min(dbHolidays.size(), 4); i++) {
                HolidayEntity h = dbHolidays.get(i);
                Map<String, Object> ev = new HashMap<>();
                ev.put("id", "hol_" + h.getId());
                ev.put("title", h.getTitle());
                ev.put("date", h.getDate() != null ? h.getDate() : "Upcoming");
                ev.put("icon", "calendar");
                ev.put("color", "#4F46E5");
                events.add(ev);
            }

            // 4. Dynamic Latest Notices from Database (Top 4 notices added from Dashboard)
            List<Map<String, Object>> notices = new ArrayList<>();
            List<NoticeEntity> dbNotices = noticeRepository.findAllByOrderByIdDesc();
            for (int i = 0; i < Math.min(dbNotices.size(), 4); i++) {
                NoticeEntity ne = dbNotices.get(i);
                Map<String, Object> n = new HashMap<>();
                n.put("id", "not_" + ne.getId());
                n.put("title", ne.getTitle());
                n.put("description", ne.getContent() != null ? ne.getContent() : ne.getTitle());
                n.put("attachmentName", (ne.getAttachment() != null && !ne.getAttachment().isBlank())
                        ? ne.getAttachment() : null);
                n.put("downloadUrl", (ne.getAttachment() != null && !ne.getAttachment().isBlank())
                        ? ("/api/files/download/" + ne.getAttachment()) : null);
                n.put("date", ne.getDate() != null && !ne.getDate().isBlank() ? ne.getDate() : "Recent");
                notices.add(n);
            }

            // 5. Dynamic School Gallery Albums from Database (Top 4 albums added from Dashboard)
            List<Map<String, Object>> gallery = new ArrayList<>();
            List<GalleryAlbumEntity> dbAlbums = galleryAlbumRepository.findByStatusIgnoreCaseOrderByDisplayOrderAsc("Active");
            if (dbAlbums.isEmpty()) {
                dbAlbums = galleryAlbumRepository.findAllByOrderByIdDesc();
            }
            for (int i = 0; i < Math.min(dbAlbums.size(), 4); i++) {
                GalleryAlbumEntity g = dbAlbums.get(i);
                Map<String, Object> gm = new HashMap<>();
                gm.put("id", g.getId());
                gm.put("title", g.getTitle());
                gm.put("photoCount", g.getPhotoCount() != null ? g.getPhotoCount() : 1);
                gm.put("coverImage", g.getCoverImage() != null ? g.getCoverImage() : "");
                gallery.add(gm);
            }

            // Sections config (SDUI Feature flags)
            Map<String, Object> sectionsConfig = new HashMap<>();
            sectionsConfig.put("showBanners", true);
            sectionsConfig.put("showSubjects", true);
            sectionsConfig.put("showUpcomingEvents", true);
            sectionsConfig.put("showLatestNotices", true);
            sectionsConfig.put("showGallery", true);
            sectionsConfig.put("showChildren", false);

            response.put("role", isParentViewingChild ? "parent_viewing_child" : "student");
            response.put("isParentViewing", isParentViewingChild);
            response.put("profile", profile);
            response.put("sectionsConfig", sectionsConfig);
            response.put("banners", banners);
            response.put("subjects", subjects);
            response.put("upcomingEvents", events);
            response.put("latestNotices", notices);
            response.put("gallery", gallery);
            response.put("children", Collections.emptyList());

            return ResponseEntity.ok(response);
        } else {
            // ================= PARENT HOME VIEW (Screenshot 3) =================
            String parentEmail = currentUser.getEmail();
            GuardianEntity guardian = guardianRepository.findByEmailIgnoreCase(parentEmail).orElse(null);
            String parentName = guardian != null && guardian.getName() != null ? guardian.getName() : currentUser.getName();
            if (parentName == null || parentName.isBlank()) {
                parentName = "";
            }
            String parentAvatar = guardian != null && guardian.getPhoto() != null ? guardian.getPhoto() : "";

            Map<String, Object> profile = new HashMap<>();
            profile.put("name", parentName);
            profile.put("email", parentEmail);
            profile.put("avatarUrl", parentAvatar);

            // Query all children for this parent
            Set<String> admissionSet = new HashSet<>();
            List<StudentEntity> childrenEntities = new ArrayList<>(studentRepository.findByGuardianEmailIgnoreCase(parentEmail));
            for (StudentEntity s : childrenEntities) {
                admissionSet.add(s.getAdmissionNo());
            }

            if (guardian != null && guardian.getStudentAdmissionNo() != null && !guardian.getStudentAdmissionNo().isBlank()) {
                String[] parts = guardian.getStudentAdmissionNo().split(",");
                for (String p : parts) {
                    String adm = p.trim();
                    if (!adm.isEmpty() && !admissionSet.contains(adm)) {
                        studentRepository.findByAdmissionNoIgnoreCase(adm).ifPresent(s -> {
                            childrenEntities.add(s);
                            admissionSet.add(adm);
                        });
                    }
                }
            }

            List<Map<String, Object>> children = new ArrayList<>();
            {
                for (StudentEntity s : childrenEntities) {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", String.valueOf(s.getId()));
                    c.put("name", s.getName());
                    String cls = s.getClassName() != null ? s.getClassName() : "";
                    c.put("className", cls.startsWith("Class") ? cls : "Class - " + cls);
                    c.put("rollNo", s.getRollNo() != null ? s.getRollNo() : "");
                    c.put("admissionNo", s.getAdmissionNo());
                    c.put("avatarUrl", s.getStudentPhoto() != null && !s.getStudentPhoto().isBlank()
                            ? s.getStudentPhoto()
                            : "");
                    children.add(c);
                }
            }

            Map<String, Object> sectionsConfig = new HashMap<>();
            sectionsConfig.put("showChildren", true);
            sectionsConfig.put("showBanners", false);
            sectionsConfig.put("showSubjects", false);
            sectionsConfig.put("showUpcomingEvents", false);
            sectionsConfig.put("showLatestNotices", false);
            sectionsConfig.put("showGallery", false);

            response.put("role", "parent");
            response.put("isParentViewing", false);
            response.put("profile", profile);
            response.put("sectionsConfig", sectionsConfig);
            response.put("children", children);
            response.put("banners", Collections.emptyList());
            response.put("subjects", Collections.emptyList());
            response.put("upcomingEvents", Collections.emptyList());
            response.put("latestNotices", Collections.emptyList());
            response.put("gallery", Collections.emptyList());

            return ResponseEntity.ok(response);
        }
    }

    private Map<String, Object> createSubjectMap(int id, String name, String code, String type, String color, String secColor, String icon) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("code", code);
        map.put("type", type);
        map.put("colorHex", color);
        map.put("secondaryColorHex", secColor);
        map.put("icon", icon);
        return map;
    }

    private Map<String, Object> mapSubjectEntityToDashboardMap(SubjectEntity sub, int index) {
        String name = sub.getName() != null ? sub.getName() : "Subject";
        String code = sub.getCode() != null ? sub.getCode() : "SUB";
        String lower = name.toLowerCase();

        String color = "#0284C7";
        String secColor = "#0369A1";
        String icon = "book";

        if (lower.contains("english")) {
            color = "#E11D48";
            secColor = "#FB7185";
            icon = "book";
        } else if (lower.contains("math")) {
            color = "#059669";
            secColor = "#34D399";
            icon = "math";
        } else if (lower.contains("science") || lower.contains("bio") || lower.contains("chem") || lower.contains("phy")) {
            color = "#65A30D";
            secColor = "#A3E635";
            icon = "science";
        } else if (lower.contains("physical") || lower.contains("sport") || lower.contains("ped")) {
            color = "#7C3AED";
            secColor = "#A78BFA";
            icon = "sports";
        } else if (lower.contains("hindi") || lower.contains("sanskrit") || lower.contains("urdu")) {
            color = "#4F46E5";
            secColor = "#818CF8";
            icon = "hindi";
        } else if (lower.contains("music") || lower.contains("art") || lower.contains("dance")) {
            color = "#EA580C";
            secColor = "#FB923C";
            icon = "music";
        } else {
            String[] palette = new String[]{"#0284C7", "#0D9488", "#D97706", "#9333EA", "#2563EB", "#0891B2"};
            color = palette[index % palette.length];
            secColor = color;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", sub.getId());
        map.put("name", name);
        map.put("code", code);
        map.put("type", "Theory");
        map.put("colorHex", color);
        map.put("secondaryColorHex", secColor);
        map.put("icon", icon);
        return map;
    }

    // ==========================================
    // 1. PROFILE ENDPOINTS
    // ==========================================
    @GetMapping("/student/teachers")
    @Operation(summary = "Get teachers assigned to the authenticated student's class and section")
    public ResponseEntity<List<Map<String,Object>>> getStudentTeachers(
            @RequestParam(required=false) String admissionNo) {
        return ResponseEntity.ok(studentTeachersService.getTeachers(assignmentStudent(admissionNo)));
    }

    @GetMapping("/student/subjects")
    @Operation(summary = "Get active subjects belonging to the enrolled student's class")
    public ResponseEntity<List<com.school.management.dto.SubjectResponse>> getStudentSubjects(
            @RequestParam(required = false) String admissionNo) {
        StudentEntity student = assignmentStudent(admissionNo);
        return ResponseEntity.ok(curriculumService.getStudentSubjects(student).stream()
                .map(com.school.management.dto.SubjectResponse::fromEntity).toList());
    }

    @GetMapping("/student/guardians")
    @Operation(summary = "Get all dashboard guardians linked to the authenticated student or selected linked child")
    public ResponseEntity<List<com.school.management.dto.GuardianResponse>> getStudentGuardians(
            @RequestParam(required = false) String admissionNo) {
        StudentEntity student = assignmentStudent(admissionNo);
        if (student.getAdmissionNo() == null || student.getAdmissionNo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student admission number missing");
        }
        return ResponseEntity.ok(guardianRepository.findLinkedToStudent(student.getAdmissionNo().trim()).stream()
                .map(g -> com.school.management.dto.GuardianResponse.fromEntity(g, student.getName())).toList());
    }

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
        Role role = securityUtil.getCurrentUser().map(UserEntity::getRole).orElse(null);
        return ResponseEntity.ok(holidayService.getAll().stream().filter(holiday -> {
            String target = holiday.getTarget() == null ? "ALL" : holiday.getTarget().trim().toUpperCase(Locale.ROOT);
            if (target.isEmpty() || target.equals("ALL")) return true;
            if (role == null) return false;
            if (Set.of(Role.ADMIN, Role.MASTER_ADMIN, Role.SUPER_ADMIN, Role.PRINCIPAL).contains(role)) return true;
            if (role == Role.STUDENT || role == Role.PARENT) return Set.of("STUDENT", "STUDENTS").contains(target);
            return Set.of("STAFF", "TEACHER", "TEACHERS").contains(target);
        }).sorted(Comparator.comparing(HolidayResponse::getDate, Comparator.nullsLast(String::compareTo))).toList());
    }

    // ==========================================
    // 3. ATTENDANCE ENDPOINTS
    // ==========================================
    @GetMapping("/attendance")
    @Operation(summary = "Get attendance records with optional role/date/className filters")
    public ResponseEntity<List<Map<String, Object>>> getAttendance(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String admissionNo) {

        UserEntity viewer = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        StudentEntity student = (viewer.getRole() == Role.STUDENT || viewer.getRole() == Role.PARENT) ? assignmentStudent(admissionNo) : null;
        List<Map<String,Object>> teacherClasses = viewer.getRole() == Role.TEACHER ? getTeacherClasses().getBody() : List.of();
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
                if (student != null && !Objects.toString(student.getAdmissionNo(), "").equalsIgnoreCase(entity.getAdmissionNo())) continue;
                if (viewer.getRole() == Role.TEACHER) {
                    if ("STUDENT".equalsIgnoreCase(entity.getAttendanceType())) {
                        StudentEntity enrolled = entity.getAdmissionNo() == null ? null : studentRepository.findByAdmissionNoIgnoreCase(entity.getAdmissionNo()).orElse(null);
                        if (enrolled == null || teacherClasses.stream().noneMatch(c -> Objects.toString(c.get("className"), "").equalsIgnoreCase(enrolled.getClassName()) &&
                                (Objects.toString(c.get("section"), "").isBlank() || Objects.toString(c.get("section"), "").equalsIgnoreCase(enrolled.getSection())))) continue;
                    } else if (!Objects.toString(viewer.getName(), "").equalsIgnoreCase(entity.getName())) continue;
                }
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
        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (user.getRole() == Role.PARENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parents cannot mark attendance");
        String date = Objects.toString(req.get("date"), LocalDate.now().toString());
        String name = Objects.toString(req.get("name"), user.getName());
        String rawType = Objects.toString(req.get("attendanceType"), Objects.toString(req.get("role"), "STUDENT"));
        String normalizedType = user.getRole() == Role.TEACHER && !"STUDENT".equalsIgnoreCase(rawType) ? "TEACHER"
                : Set.of(Role.STAFF, Role.ACCOUNTANT, Role.LIBRARIAN).contains(user.getRole()) ? "EMPLOYEE"
                : "TEACHER".equalsIgnoreCase(rawType) ? "TEACHER" : "STUDENT";
        if (user.getRole() == Role.STUDENT) normalizedType = "STUDENT";
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
        String admissionNo = req.get("admissionNo") == null ? null : req.get("admissionNo").toString().trim();
        if ("STUDENT".equals(normalizedType)) {
            StudentEntity enrollment;
            if (user.getRole() == Role.STUDENT) enrollment = studentRepository.findByEmailIgnoreCase(user.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not linked"));
            else enrollment = studentRepository.findByAdmissionNoIgnoreCase(Objects.toString(admissionNo, ""))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an enrolled student with admission number"));
            if (user.getRole() == Role.TEACHER) requireAssignedClass(enrollment.getClassName(), enrollment.getSection());
            admissionNo = enrollment.getAdmissionNo(); name = enrollment.getName(); rollNo = enrollment.getRollNo(); className = enrollment.getClassName();
        } else if (!Set.of(Role.ADMIN, Role.MASTER_ADMIN, Role.SUPER_ADMIN, Role.PRINCIPAL).contains(user.getRole())) {
            name = user.getName(); admissionNo = null;
        }
        if (!Set.of("present", "absent", "late", "holiday").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance status");
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

        String identity = admissionNo;
        String recordName = name;
        AttendanceEntity previous = attendanceRepository.findByAttendanceTypeAndAttendanceDate(normalizedType, date).stream()
                .filter(record -> identity != null ? identity.equalsIgnoreCase(record.getAdmissionNo()) : recordName.equalsIgnoreCase(record.getName())).findFirst().orElse(null);
        if (previous != null) {
            entityToSave.setId(previous.getId()); entityToSave.setCreatedAt(previous.getCreatedAt());
            if (checkInTime == null) entityToSave.setCheckInTime(previous.getCheckInTime());
            if (checkOutTime == null) entityToSave.setCheckOutTime(previous.getCheckOutTime());
        }
        entityToSave = attendanceRepository.save(entityToSave);

        Map<String, Object> record = new HashMap<>();
        record.put("id", entityToSave.getId());
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

        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (user.getRole() == Role.STUDENT || user.getRole() == Role.PARENT) return getStudentAssignments(null);
        return getTeacherAssignments(className, null);
    }

    @PostMapping("/homework/submit/{id}")
    @Operation(summary = "Submit homework by student")
    public ResponseEntity<Map<String, Object>> submitHomework(@PathVariable String id) {
        String cleanId = id.replace("hw_", "");
        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (user.getRole() != Role.STUDENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can submit homework");
        StudentEntity student = studentRepository.findByEmailIgnoreCase(user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not linked"));
        Long homeworkId;
        try { homeworkId = Long.valueOf(cleanId); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid homework ID"); }
        HomeworkEntity homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Homework not found"));
        if (!assignedTo(homework, student))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Homework is not assigned to your class");
        HomeworkSubmissionEntity submission = homeworkSubmissionRepository.findByHomeworkIdAndStudentId(homeworkId, student.getId()).orElseGet(() -> {
            HomeworkSubmissionEntity created = new HomeworkSubmissionEntity();
            created.setHomeworkId(homeworkId);
            created.setStudentId(student.getId());
            created.setSubmittedAt(LocalDateTime.now());
            return created;
        });
        submission.setSubmittedAt(LocalDateTime.now());
        if (Set.of("GRADED", "ACCEPTED").contains(Objects.toString(submission.getStatus(), "").toUpperCase()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reviewed work cannot be replaced unless the teacher rejects it for resubmission");
        submission.setStudentName(student.getName());
        submission.setStudentRollNo(student.getRollNo());
        submission.setStudentAdmissionNo(student.getAdmissionNo());
        submission.setStatus("SUBMITTED");
        submission.setPoints(null); submission.setTeacherFeedback(null); submission.setReviewedAt(null);
        homeworkSubmissionRepository.save(submission);

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "submitted");
        res.put("message", "Homework submitted successfully");
        return ResponseEntity.ok(res);
    }

    // ==========================================
    // 4A. TEACHER CLASSES & STUDENTS ENDPOINTS
    // ==========================================
    @GetMapping("/teacher/classes")
    @Operation(summary = "Get teacher assigned classes with student counts")
    public ResponseEntity<List<Map<String, Object>>> getTeacherClasses() {
        UserEntity user = assignmentManager();
        List<Map<String, Object>> classesList = new ArrayList<>();

        TeacherEntity teacher = null;
        if (user != null && user.getRole() == Role.TEACHER) {
            teacher = teacherRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        }

        Set<String> classNames = new LinkedHashSet<>();
        if (teacher != null && teacher.getAssignedClasses() != null && !teacher.getAssignedClasses().isEmpty()) {
            for (SchoolClassEntity sc : teacher.getAssignedClasses()) {
                classNames.add(sc.getName() + (sc.getSection() != null ? " - " + sc.getSection() : ""));
            }
        } else if (teacher != null && teacher.getAssignedClass() != null && !teacher.getAssignedClass().isBlank()) {
            String[] parts = teacher.getAssignedClass().split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) classNames.add(p.trim());
            }
        }

        if (classNames.isEmpty() && user.getRole() != Role.TEACHER) {
            List<SchoolClassEntity> scList = schoolClassRepository.findAll();
            for (SchoolClassEntity sc : scList) {
                classNames.add(sc.getName() + (sc.getSection() != null ? " - " + sc.getSection() : ""));
            }
        }

        if (classNames.isEmpty() && user.getRole() != Role.TEACHER) {
            List<StudentEntity> students = studentRepository.findAll();
            for (StudentEntity s : students) {
                if (s.getClassName() != null && !s.getClassName().isBlank()) {
                    classNames.add(s.getClassName() + (s.getSection() != null && !s.getSection().isBlank() ? " - " + s.getSection() : ""));
                }
            }
        }

        long idx = 1;
        String teacherSubject = teacher != null && teacher.getSubject() != null ? teacher.getSubject() : "All Subjects";
        for (String cName : classNames) {
            String tempBaseClass = cName;
            String tempSection = "";
            if (cName.contains("-")) {
                String[] parts = cName.split("-");
                tempBaseClass = parts[0].trim();
                if (parts.length > 1) tempSection = parts[1].trim();
            }
            final String baseClass = tempBaseClass;
            final String section = tempSection;

            long totalStudents = studentRepository.findByClassNameIgnoreCase(baseClass).stream()
                    .filter(s -> section.isBlank() || section.equalsIgnoreCase(s.getSection()))
                    .count();
            if (totalStudents == 0) {
                totalStudents = studentRepository.countByClassName(baseClass);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", "cls_" + idx++);
            map.put("className", baseClass);
            map.put("section", section);
            map.put("subject", teacherSubject);
            map.put("totalStudents", totalStudents);
            map.put("room", "");
            map.put("isClassTeacher", false);
            map.put("timing", "");
            map.put("syllabusProgress", 0.0);
            classesList.add(map);
        }

        return ResponseEntity.ok(classesList);
    }

    @GetMapping("/classes/{className}/students")
    @Operation(summary = "Get enrolled students list in a class with ID and details")
    public ResponseEntity<List<Map<String, Object>>> getClassStudents(
            @PathVariable String className,
            @RequestParam(required = false) String section) {

        requireAssignedClass(className, section);
        List<StudentEntity> list;
        if (section != null && !section.isBlank()) {
            list = studentRepository.findByClassNameIgnoreCaseAndSectionIgnoreCase(className.trim(), section.trim());
        } else {
            list = studentRepository.findByClassNameIgnoreCase(className.trim());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentEntity s : list) {
            Map<String, Object> sm = new HashMap<>();
            sm.put("id", s.getId());
            sm.put("admissionNo", s.getAdmissionNo());
            sm.put("rollNo", s.getRollNo());
            sm.put("name", s.getName() != null ? s.getName() : (s.getFirstName() + " " + s.getLastName()).trim());
            sm.put("className", s.getClassName());
            sm.put("section", s.getSection());
            sm.put("gender", s.getGender() != null ? s.getGender().name() : "MALE");
            sm.put("phone", s.getPhone());
            sm.put("email", s.getEmail());
            sm.put("studentPhoto", s.getStudentPhoto());
            result.add(sm);
        }
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // 4B. TEACHER LESSONS & TOPICS ENDPOINTS
    // ==========================================
    @GetMapping("/teacher/lessons")
    @Operation(summary = "Get lessons/chapters for teacher per class and subject")
    public ResponseEntity<List<Map<String, Object>>> getTeacherLessons(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) Long subjectId) {

        List<SubjectChapterEntity> chapters;
        if (subjectId != null) {
            if (className != null && !className.isBlank()) {
                chapters = subjectChapterRepository.findBySubjectIdAndClassNameFlexible(subjectId, className.trim());
            } else {
                chapters = subjectChapterRepository.findBySubjectIdOrderByDisplayOrderAsc(subjectId);
            }
        } else if (className != null && !className.isBlank()) {
            chapters = subjectChapterRepository.findByClassNameFlexible(className.trim());
        } else {
            chapters = subjectChapterRepository.findAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (SubjectChapterEntity ch : chapters) {
            List<SubjectTopicEntity> topics = subjectTopicRepository.findByChapterIdOrderByDisplayOrderAsc(ch.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", ch.getId());
            map.put("chapterNumber", ch.getChapterNumber());
            map.put("chapterTitle", ch.getChapterTitle());
            map.put("title", ch.getChapterNumber() + ": " + ch.getChapterTitle());
            map.put("description", ch.getDescription());
            map.put("className", ch.getClassName());
            map.put("subjectId", ch.getSubject() != null ? ch.getSubject().getId() : null);
            map.put("subjectName", ch.getSubject() != null ? ch.getSubject().getName() : "General");
            map.put("topicsCount", topics.size());

            List<Map<String, Object>> topicMaps = new ArrayList<>();
            for (SubjectTopicEntity t : topics) {
                Map<String, Object> tm = new HashMap<>();
                tm.put("id", t.getId());
                tm.put("title", t.getTitle());
                tm.put("description", t.getDescription());
                tm.put("difficulty", t.getDifficulty());
                tm.put("estimatedMinutes", t.getEstimatedMinutes());
                tm.put("status", t.getStatus());
                topicMaps.add(tm);
            }
            map.put("topics", topicMaps);
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/teacher/lessons")
    @Operation(summary = "Create a new lesson/chapter")
    public ResponseEntity<Map<String, Object>> createTeacherLesson(@RequestBody Map<String, Object> req) {
        assignmentManager();
        String className = Objects.toString(req.get("className"), "").trim();
        String chapterNumber = Objects.toString(req.get("chapterNumber"), "").trim();
        String chapterTitle = Objects.toString(req.get("chapterTitle"), "").trim();
        String description = Objects.toString(req.get("description"), "");
        if (className.isBlank() || chapterTitle.isBlank() || req.get("subjectId") == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class, title and subject are required");
        Long subjectId = Long.valueOf(req.get("subjectId").toString());
        SubjectEntity subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));

        SubjectChapterEntity chapter = SubjectChapterEntity.builder()
                .subject(subject)
                .className(className)
                .chapterNumber(chapterNumber)
                .chapterTitle(chapterTitle)
                .description(description)
                .displayOrder(1)
                .build();
        chapter = subjectChapterRepository.save(chapter);

        Map<String, Object> res = new HashMap<>();
        res.put("id", chapter.getId());
        res.put("chapterNumber", chapter.getChapterNumber());
        res.put("chapterTitle", chapter.getChapterTitle());
        res.put("className", chapter.getClassName());
        res.put("description", chapter.getDescription());
        res.put("topicsCount", 0);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/teacher/lessons/{lessonId}/topics")
    @Operation(summary = "Add a topic to a lesson")
    public ResponseEntity<Map<String, Object>> addTopicToLesson(
            @PathVariable Long lessonId,
            @RequestBody Map<String, Object> req) {

        SubjectChapterEntity chapter = subjectChapterRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        String title = String.valueOf(req.getOrDefault("title", "New Topic"));
        String desc = String.valueOf(req.getOrDefault("description", ""));
        String difficulty = String.valueOf(req.getOrDefault("difficulty", "Medium"));
        int estMinutes = Integer.parseInt(String.valueOf(req.getOrDefault("estimatedMinutes", 30)));

        SubjectTopicEntity topic = SubjectTopicEntity.builder()
                .chapter(chapter)
                .subject(chapter.getSubject())
                .className(chapter.getClassName())
                .title(title)
                .description(desc)
                .difficulty(difficulty)
                .estimatedMinutes(estMinutes)
                .status("Active")
                .build();
        topic = subjectTopicRepository.save(topic);

        Map<String, Object> res = new HashMap<>();
        res.put("id", topic.getId());
        res.put("title", topic.getTitle());
        res.put("description", topic.getDescription());
        res.put("difficulty", topic.getDifficulty());
        res.put("estimatedMinutes", topic.getEstimatedMinutes());
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/teacher/lessons/{lessonId}")
    @Operation(summary = "Delete a lesson/chapter")
    public ResponseEntity<Map<String, Object>> deleteTeacherLesson(@PathVariable Long lessonId) {
        List<SubjectTopicEntity> topics = subjectTopicRepository.findByChapterIdOrderByDisplayOrderAsc(lessonId);
        subjectTopicRepository.deleteAll(topics);
        subjectChapterRepository.deleteById(lessonId);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Lesson deleted successfully");
        return ResponseEntity.ok(res);
    }

    // ==========================================
    // 4C. TEACHER ASSIGNMENTS (ID-WISE ASSIGNMENT & GRADING)
    // ==========================================
    @GetMapping("/teacher/assignments")
    @Operation(summary = "Get assignments created by teacher with submission stats")
    public ResponseEntity<List<Map<String, Object>>> getTeacherAssignments(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String subject) {

        UserEntity user = assignmentManager();
        TeacherEntity teacher = user.getRole() == Role.TEACHER
                ? teacherRepository.findByEmailIgnoreCase(user.getEmail()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not linked")) : null;
        List<HomeworkEntity> list = homeworkRepository.findAllByOrderByIdDesc().stream()
                .filter(h -> teacher == null || Objects.equals(h.getTeacherId(), teacher.getId())).toList();
        if (className != null && !className.isBlank()) {
            list = list.stream().filter(h -> className.equalsIgnoreCase(h.getClassName())).toList();
        }
        if (subject != null && !subject.isBlank()) {
            list = list.stream().filter(h -> subject.equalsIgnoreCase(h.getSubject())).toList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (HomeworkEntity h : list) {
            List<HomeworkSubmissionEntity> submissions = homeworkSubmissionRepository.findByHomeworkIdOrderByIdDesc(h.getId());
            long evaluatedCount = submissions.stream().filter(s -> s.getPoints() != null || "GRADED".equalsIgnoreCase(s.getStatus()) || "ACCEPTED".equalsIgnoreCase(s.getStatus())).count();

            long totalStudents = studentRepository.findByClassNameIgnoreCase(h.getClassName()).stream()
                    .filter(student -> assignedTo(h, student)).count();

            Map<String, Object> map = new HashMap<>();
            map.put("id", "hw_" + h.getId());
            map.put("dbId", h.getId());
            map.put("title", h.getTitle());
            map.put("description", h.getDescription());
            map.put("subject", h.getSubject());
            map.put("className", h.getClassName());
            map.put("section", h.getSection());
            map.put("dueDate", h.getDueDate());
            map.put("assignedDate", h.getAssignedDate());
            map.put("assignedBy", h.getAssignedBy());
            map.put("points", h.getPoints() != null ? h.getPoints() : 100);
            map.put("targetStudentId", h.getTargetStudentId());
            map.put("targetStudentName", h.getTargetStudentName());
            map.put("targetStudentAdmissionNo", h.getTargetStudentAdmissionNo());
            map.put("isTargetedToStudent", h.getTargetStudentId() != null);
            map.put("fileAttachmentUrl", h.getFileAttachmentUrl());
            map.put("submissionCount", submissions.size());
            map.put("evaluatedCount", evaluatedCount);
            map.put("totalStudents", totalStudents);
            map.put("status", h.getStatus());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/teacher/assignments")
    @Operation(summary = "Create an assignment and assign class-wide or to a specific Student ID")
    public ResponseEntity<Map<String, Object>> createTeacherAssignment(@RequestBody Map<String, Object> req) {
        return persistAssignment(req, null);
    }

    @PutMapping("/teacher/assignments/{id}")
    public ResponseEntity<Map<String, Object>> updateTeacherAssignment(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return persistAssignment(req, managedAssignment(id));
    }

    @DeleteMapping("/teacher/assignments/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteTeacherAssignment(@PathVariable String id) {
        HomeworkEntity homework = managedAssignment(id);
        homeworkSubmissionRepository.deleteAll(homeworkSubmissionRepository.findByHomeworkIdOrderByIdDesc(homework.getId()));
        homeworkRepository.delete(homework);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Map<String, Object>> persistAssignment(Map<String, Object> req, HomeworkEntity existing) {
        UserEntity user = assignmentManager();
        TeacherEntity teacher = user.getRole() == Role.TEACHER
                ? teacherRepository.findByEmailIgnoreCase(user.getEmail()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher profile not linked")) : null;
        String title = Objects.toString(req.get("title"), "").trim();
        String description = Objects.toString(req.get("description"), "").trim();
        String subject = Objects.toString(req.get("subject"), "").trim();
        String className = Objects.toString(req.get("className"), "").trim();
        String section = Objects.toString(req.get("section"), "").trim();
        String dueDate = Objects.toString(req.get("dueDate"), "").trim();
        if (title.isBlank() || subject.isBlank() || className.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title, subject and class are required");
        int points;
        try {
            if (LocalDate.parse(dueDate).isBefore(LocalDate.now()) && (existing == null || !dueDate.equals(existing.getDueDate()))) throw new IllegalArgumentException();
            points = Integer.parseInt(Objects.toString(req.getOrDefault("points", 100)));
            if (points < 1) throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a valid future due date and positive maximum points"); }
        requireAssignedClass(className, section);
        if (schoolClassRepository.findAll().stream().noneMatch(c -> c.getName().equalsIgnoreCase(className) &&
                (section.isBlank() || section.equalsIgnoreCase(c.getSection()))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class/section does not exist");
        String fileAttachmentUrl = req.containsKey("fileAttachmentUrl") && req.get("fileAttachmentUrl") != null
                ? req.get("fileAttachmentUrl").toString() : null;

        Long targetStudentId = null;
        String targetStudentName = null;
        String targetStudentAdmissionNo = null;

        if (req.containsKey("targetStudentId") && req.get("targetStudentId") != null && !req.get("targetStudentId").toString().isBlank()) {
            try { targetStudentId = Long.valueOf(req.get("targetStudentId").toString()); }
            catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid student ID"); }
            StudentEntity targetStudent = studentRepository.findById(targetStudentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
            if (!className.equalsIgnoreCase(targetStudent.getClassName()) || (!section.isBlank() && !section.equalsIgnoreCase(targetStudent.getSection())))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not enrolled in selected class/section");
            targetStudentName = targetStudent.getName();
            targetStudentAdmissionNo = targetStudent.getAdmissionNo();
        }

        if (existing != null && !homeworkSubmissionRepository.findByHomeworkIdOrderByIdDesc(existing.getId()).isEmpty() &&
                (!Objects.equals(existing.getClassName(), className) || !Objects.equals(Objects.toString(existing.getSection(), ""), section) || !Objects.equals(existing.getTargetStudentId(), targetStudentId)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assignment audience cannot change after students have submitted work");
        if (existing != null && homeworkSubmissionRepository.findByHomeworkIdOrderByIdDesc(existing.getId()).stream()
                .anyMatch(submission -> submission.getPoints() != null && submission.getPoints() > points))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Maximum points cannot be lower than an existing reviewed score");
        HomeworkEntity homework = HomeworkEntity.builder()
                .id(existing == null ? null : existing.getId())
                .createdAt(existing == null ? null : existing.getCreatedAt())
                .title(title)
                .description(description)
                .subject(subject)
                .className(className)
                .section(section)
                .assignedDate(existing == null ? LocalDate.now().toString() : existing.getAssignedDate())
                .dueDate(dueDate)
                .status("ACTIVE")
                .assignedBy(existing != null ? existing.getAssignedBy() : user.getName())
                .teacherId(existing != null ? existing.getTeacherId() : (teacher != null ? teacher.getId() : null))
                .points(points)
                .targetStudentId(targetStudentId)
                .targetStudentName(targetStudentName)
                .targetStudentAdmissionNo(targetStudentAdmissionNo)
                .fileAttachmentUrl(fileAttachmentUrl)
                .build();
        homework = homeworkRepository.save(homework);
        final HomeworkEntity published = homework;
        studentRepository.findByClassNameIgnoreCase(homework.getClassName()).stream().filter(student -> assignedTo(published, student))
            .forEach(student -> portalNotifications.family(student, existing==null?"Homework assigned":"Homework updated", published.getTitle(), "homework", published.getId().toString()));

        Map<String, Object> res = new HashMap<>();
        res.put("id", "hw_" + homework.getId());
        res.put("dbId", homework.getId());
        res.put("title", homework.getTitle());
        res.put("description", homework.getDescription());
        res.put("subject", homework.getSubject());
        res.put("className", homework.getClassName());
        res.put("section", homework.getSection());
        res.put("dueDate", homework.getDueDate());
        res.put("points", homework.getPoints());
        res.put("targetStudentId", homework.getTargetStudentId());
        res.put("targetStudentName", homework.getTargetStudentName());
        res.put("message", "Assignment created and assigned successfully");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/teacher/assignments/{id}/submissions")
    @Operation(summary = "Get all student submissions for an assignment")
    public ResponseEntity<List<Map<String, Object>>> getAssignmentSubmissions(@PathVariable String id) {
        String cleanId = id.replace("hw_", "");
        Long homeworkId = Long.valueOf(cleanId);

        HomeworkEntity hw = managedAssignment(id);

        List<HomeworkSubmissionEntity> submissions = homeworkSubmissionRepository.findByHomeworkIdOrderByIdDesc(homeworkId);

        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> submittedStudentIds = new HashSet<>();

        for (HomeworkSubmissionEntity s : submissions) {
            submittedStudentIds.add(s.getStudentId());
            Map<String, Object> map = new HashMap<>();
            map.put("submissionId", s.getId());
            map.put("homeworkId", homeworkId);
            map.put("studentId", s.getStudentId());
            map.put("studentName", s.getStudentName() != null ? s.getStudentName() : "Student #" + s.getStudentId());
            map.put("studentRollNo", s.getStudentRollNo() != null ? s.getStudentRollNo() : "");
            map.put("studentAdmissionNo", s.getStudentAdmissionNo() != null ? s.getStudentAdmissionNo() : "");
            map.put("submittedAt", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "");
            map.put("submissionText", s.getSubmissionText() != null ? s.getSubmissionText() : "");
            map.put("fileUrl", s.getFileUrl());
            map.put("status", s.getStatus() != null ? s.getStatus() : "SUBMITTED");
            map.put("points", s.getPoints());
            map.put("maxPoints", hw.getPoints() != null ? hw.getPoints() : 100);
            map.put("teacherFeedback", s.getTeacherFeedback());
            map.put("reviewedAt", s.getReviewedAt() != null ? s.getReviewedAt().toString() : null);
            result.add(map);
        }

        List<StudentEntity> eligibleStudents = new ArrayList<>();
        if (hw.getTargetStudentId() != null) {
            studentRepository.findById(hw.getTargetStudentId()).ifPresent(eligibleStudents::add);
        } else if (hw.getClassName() != null) {
            eligibleStudents = studentRepository.findByClassNameIgnoreCase(hw.getClassName());
        }

        for (StudentEntity st : eligibleStudents) {
            if (!assignedTo(hw, st)) continue;
            if (!submittedStudentIds.contains(st.getId())) {
                Map<String, Object> map = new HashMap<>();
                map.put("submissionId", null);
                map.put("homeworkId", homeworkId);
                map.put("studentId", st.getId());
                map.put("studentName", st.getName() != null ? st.getName() : (st.getFirstName() + " " + st.getLastName()).trim());
                map.put("studentRollNo", st.getRollNo());
                map.put("studentAdmissionNo", st.getAdmissionNo());
                map.put("submittedAt", null);
                map.put("submissionText", null);
                map.put("fileUrl", null);
                map.put("status", "PENDING");
                map.put("points", null);
                map.put("maxPoints", hw.getPoints() != null ? hw.getPoints() : 100);
                map.put("teacherFeedback", null);
                map.put("reviewedAt", null);
                result.add(map);
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/teacher/assignments/{id}/submissions/{submissionId}/review")
    @Operation(summary = "Teacher reviews and grades a student submission")
    public ResponseEntity<Map<String, Object>> reviewSubmission(
            @PathVariable String id,
            @PathVariable Long submissionId,
            @RequestBody Map<String, Object> req) {

        HomeworkEntity hw = managedAssignment(id);
        HomeworkSubmissionEntity submission = homeworkSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (!Objects.equals(submission.getHomeworkId(), hw.getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission does not belong to this assignment");
        String status = String.valueOf(req.getOrDefault("status", "ACCEPTED")).toUpperCase();
        Integer points;
        try { points = req.get("points") == null ? null : Integer.valueOf(req.get("points").toString()); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score must be an integer"); }
        String feedback = req.containsKey("feedback") && req.get("feedback") != null
                ? req.get("feedback").toString() : null;

        if (!Set.of("ACCEPTED", "REJECTED", "GRADED").contains(status) ||
                (points != null && (points < 0 || points > hw.getPoints())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review status or score");
        submission.setStatus(status);
        if ("REJECTED".equals(status)) submission.setPoints(null);
        else if (points != null) submission.setPoints(points);
        if (feedback != null) {
            submission.setTeacherFeedback(feedback);
        }
        submission.setReviewedAt(LocalDateTime.now());
        submission = homeworkSubmissionRepository.save(submission);
        studentRepository.findById(submission.getStudentId()).ifPresent(student -> portalNotifications.family(student, "Homework reviewed", hw.getTitle(), "homework", hw.getId().toString()));

        Map<String, Object> res = new HashMap<>();
        res.put("submissionId", submission.getId());
        res.put("status", submission.getStatus());
        res.put("points", submission.getPoints());
        res.put("feedback", submission.getTeacherFeedback());
        res.put("reviewedAt", submission.getReviewedAt().toString());
        res.put("message", "Submission evaluated successfully");
        return ResponseEntity.ok(res);
    }

    // ==========================================
    // 4D. STUDENT & PARENT ASSIGNMENTS (WITH ID-WISE FILTER & SUBMISSION)
    // ==========================================
    @GetMapping("/student/assignments")
    @Operation(summary = "Get assignments for student (class-wide + personalized by student ID)")
    public ResponseEntity<List<Map<String, Object>>> getStudentAssignments(
            @RequestParam(required = false) String admissionNo) {

        StudentEntity student = assignmentStudent(admissionNo);

        List<HomeworkEntity> allHw = homeworkRepository.findAllByOrderByIdDesc();
        final StudentEntity currentStudent = student;

        List<HomeworkEntity> eligible = allHw.stream().filter(h -> assignedTo(h, currentStudent)).toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (HomeworkEntity h : eligible) {
            Optional<HomeworkSubmissionEntity> subOpt = homeworkSubmissionRepository.findByHomeworkIdAndStudentId(h.getId(), currentStudent.getId());

            String submissionStatus = "pending";
            Integer scoredPoints = null;
            String teacherFeedback = null;
            String submittedAt = null;
            String submissionText = null;
            String submissionFileUrl = null;

            if (subOpt.isPresent()) {
                HomeworkSubmissionEntity sub = subOpt.get();
                submissionStatus = sub.getStatus() != null ? sub.getStatus().toLowerCase() : "submitted";
                scoredPoints = sub.getPoints();
                teacherFeedback = sub.getTeacherFeedback();
                submittedAt = sub.getSubmittedAt() != null ? sub.getSubmittedAt().toString() : null;
                submissionText = sub.getSubmissionText();
                submissionFileUrl = sub.getFileUrl();
            } else if (h.getDueDate() != null) {
                try {
                    if (LocalDate.parse(h.getDueDate()).isBefore(LocalDate.now())) {
                        submissionStatus = "overdue";
                    }
                } catch (Exception ignored) {}
            }

            boolean isPersonalized = (h.getTargetStudentId() != null && h.getTargetStudentId().equals(currentStudent.getId()));

            Map<String, Object> item = new HashMap<>();
            item.put("id", "hw_" + h.getId());
            item.put("dbId", h.getId());
            item.put("title", h.getTitle());
            item.put("description", h.getDescription());
            item.put("subject", h.getSubject());
            item.put("className", h.getClassName());
            item.put("section", h.getSection());
            item.put("dueDate", h.getDueDate());
            item.put("assignedDate", h.getAssignedDate());
            item.put("assignedBy", h.getAssignedBy());
            item.put("points", h.getPoints() != null ? h.getPoints() : 100);
            item.put("maxPoints", h.getPoints() != null ? h.getPoints() : 100);
            item.put("fileAttachmentUrl", h.getFileAttachmentUrl());
            item.put("isPersonalized", isPersonalized);
            item.put("targetStudentId", h.getTargetStudentId());
            item.put("targetStudentName", h.getTargetStudentName());
            item.put("status", submissionStatus);
            item.put("submissionStatus", submissionStatus);
            item.put("scoredPoints", scoredPoints);
            item.put("teacherFeedback", teacherFeedback);
            item.put("submittedAt", submittedAt);
            item.put("submissionText", submissionText);
            item.put("submissionFileUrl", submissionFileUrl);
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/student/assignments/{id}/submit")
    @Operation(summary = "Submit assignment work with text and attachment link")
    public ResponseEntity<Map<String, Object>> submitStudentAssignment(
            @PathVariable String id,
            @RequestBody Map<String, Object> req) {

        String cleanId = id.replace("hw_", "");
        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (user.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can submit assignments");
        }

        StudentEntity student = studentRepository.findByEmailIgnoreCase(user.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not linked"));

        Long homeworkId = Long.valueOf(cleanId);
        HomeworkEntity homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!assignedTo(homework, student)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assignment is not assigned to you");
        String text = String.valueOf(req.getOrDefault("submissionText", ""));
        String fileUrl = req.containsKey("fileUrl") && req.get("fileUrl") != null ? req.get("fileUrl").toString() : null;
        if (text.isBlank() && (fileUrl == null || fileUrl.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide an answer or attachment");

        HomeworkSubmissionEntity submission = homeworkSubmissionRepository.findByHomeworkIdAndStudentId(homeworkId, student.getId())
                .orElseGet(() -> HomeworkSubmissionEntity.builder()
                        .homeworkId(homeworkId)
                        .studentId(student.getId())
                        .build());

        if (Set.of("GRADED", "ACCEPTED").contains(Objects.toString(submission.getStatus(), "").toUpperCase()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reviewed work cannot be replaced unless the teacher rejects it for resubmission");
        submission.setStudentName(student.getName());
        submission.setStudentRollNo(student.getRollNo());
        submission.setStudentAdmissionNo(student.getAdmissionNo());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setSubmissionText(text);
        submission.setFileUrl(fileUrl);
        submission.setStatus("SUBMITTED");
        submission.setPoints(null);
        submission.setTeacherFeedback(null);
        submission.setReviewedAt(null);
        submission = homeworkSubmissionRepository.save(submission);
        if(homework.getTeacherId()!=null) teacherRepository.findById(homework.getTeacherId()).ifPresent(teacher ->
            portalUsers.findByEmailIgnoreCase(teacher.getEmail()).ifPresent(account -> portalNotifications.send(account.getId(), "Homework submitted", student.getName()+": "+homework.getTitle(), "homework", homework.getId().toString())));

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "submitted");
        res.put("submittedAt", submission.getSubmittedAt().toString());
        res.put("message", "Assignment submitted successfully!");
        return ResponseEntity.ok(res);
    }

    // ==========================================
    // 5. FEES ENDPOINTS (Database-Backed & Live Sync)
    // ==========================================
    @GetMapping("/fees")
    @Operation(summary = "Get fee records backed by PostgreSQL database")
    public ResponseEntity<List<Map<String, Object>>> getFees(@RequestParam(required = false) String admissionNo) {
        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        String targetAdmissionNo = (user.getRole() == Role.STUDENT || user.getRole() == Role.PARENT)
                ? assignmentStudent(admissionNo).getAdmissionNo() : null;

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
                map.put("transactionId", f.getOnlineReceipt() != null ? f.getOnlineReceipt() : (f.getNote() != null && f.getNote().startsWith("TXN-") ? f.getNote() : null));
            }
            fees.add(map);
        }

        return ResponseEntity.ok(fees);
    }

    @PostMapping("/fees/pay/{id}")
    @Operation(summary = "Online fee payment requires a configured, server-verified payment provider")
    public ResponseEntity<Map<String, Object>> payFee(@PathVariable String id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Online payment is not configured. Contact the school to pay and receive a verified receipt.");
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
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String admissionNo) {

        UserEntity user = securityUtil.getCurrentUser().orElse(null);
        String resolvedClass = className;
        String resolvedSection = section;
        String resolvedTeacher = teacherName;

        if (user == null) throw new UnauthorizedException("Not authenticated");
        if (user.getRole() == Role.STUDENT || user.getRole() == Role.PARENT) {
            StudentEntity student = assignmentStudent(admissionNo);
            resolvedClass = student.getClassName();
            resolvedSection = student.getSection();
            resolvedTeacher = null;
        } else if (user.getRole() == Role.TEACHER) {
            TeacherEntity teacher = teacherRepository.findByEmailIgnoreCase(user.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not linked"));
            resolvedTeacher = (teacher.getFirstName() + " " + teacher.getLastName()).trim();
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
                if (resolvedSection == null || resolvedSection.isBlank()) {
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
    public ResponseEntity<List<Map<String, Object>>> getExams(@RequestParam(required = false) String admissionNo) {
        UserEntity viewer = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        StudentEntity selectedStudent = (viewer.getRole() == Role.STUDENT || viewer.getRole() == Role.PARENT) ? assignmentStudent(admissionNo) : null;
        List<Map<String, Object>> exams = new ArrayList<>();

        // 1. Exam schedules from DB
        List<ExamScheduleEntity> schedules = examScheduleRepository.findAllByOrderByIdDesc();
        for (ExamScheduleEntity s : schedules) {
            if (selectedStudent != null && (!Objects.toString(selectedStudent.getClassName(), "").equalsIgnoreCase(s.getClassName()) ||
                    (s.getSection() != null && !s.getSection().isBlank() && !s.getSection().equalsIgnoreCase(selectedStudent.getSection())))) continue;
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
                    s.getMaxMarks() == null ? 100.0 : s.getMaxMarks(),
                    null,
                    "upcoming",
                    s.getClassName() != null ? s.getClassName() : ""));
            exams.get(exams.size() - 1).put("section", s.getSection());
        }

        // 2. Exam results from DB
        String targetAdmissionNo = selectedStudent != null ? selectedStudent.getAdmissionNo() : null;

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
                    "",
                    "",
                    "",
                    r.getMaxMarks() == null ? 100.0 : r.getMaxMarks(),
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
        assignmentManager();
        String title = Objects.toString(req.get("title"), "").trim();
        String desc = String.valueOf(req.getOrDefault("description", ""));
        String date = String.valueOf(req.getOrDefault("date", LocalDate.now().toString()));
        String start = Objects.toString(req.get("startTime"), "").trim();
        String end = Objects.toString(req.get("endTime"), "").trim();
        String room = String.valueOf(req.getOrDefault("room", ""));
        double maxMarks = Double.parseDouble(String.valueOf(req.getOrDefault("maxMarks", 100.0)));
        String className = String.valueOf(req.getOrDefault("className", ""));

        String section = Objects.toString(req.get("section"), "").trim();
        if (title.isBlank() || subject.isBlank() || className.isBlank() || start.isBlank() || end.isBlank() || maxMarks <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide title, subject, class, times and positive maximum marks");
        requireAssignedClass(className, section);
        ExamScheduleEntity entity = ExamScheduleEntity.builder()
                .examName(title)
                .className(className)
                .section(section)
                .subject(subject)
                .date(date)
                .startTime(start)
                .endTime(end)
                .duration(Objects.toString(req.get("duration"), ""))
                .maxMarks(maxMarks)
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
        assignmentManager();
        Long scheduleId;
        double score;
        try {
            if (id.startsWith("res_")) {
                ExamResultEntity existing = examResultRepository.findById(Long.valueOf(id.substring(4)))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
                scheduleId = existing.getScheduleId();
                if (scheduleId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Update legacy results through the dashboard");
            } else scheduleId = Long.valueOf(id.replace("sched_", ""));
            score = Double.parseDouble(Objects.toString(req.get("scoredMarks"), ""));
        }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid schedule ID and score are required"); }
        ExamScheduleEntity schedule = examScheduleRepository.findById(scheduleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam schedule not found"));
        requireAssignedClass(schedule.getClassName(), schedule.getSection());
        double maximum = schedule.getMaxMarks() == null ? 100 : schedule.getMaxMarks();
        if (!Double.isFinite(score) || score < 0 || score > maximum) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score is outside the allowed range");
        String admission = Objects.toString(req.get("admissionNo"), "").trim();
        StudentEntity student = studentRepository.findByAdmissionNoIgnoreCase(admission).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an enrolled student"));
        if (!schedule.getClassName().equalsIgnoreCase(student.getClassName()) || (schedule.getSection() != null && !schedule.getSection().isBlank() && !schedule.getSection().equalsIgnoreCase(student.getSection())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not enrolled in this exam class/section");
        int percent = (int) Math.round(score / maximum * 100);
        String grade = percent >= 90 ? "A+" : percent >= 80 ? "A" : percent >= 70 ? "B" : percent >= 50 ? "C" : "D";
        ExamResultEntity resultEntity = examResultRepository.findByAdmissionNoIgnoreCaseOrderByIdDesc(admission).stream()
                .filter(result -> scheduleId.equals(result.getScheduleId())).findFirst().orElseGet(ExamResultEntity::new);
        resultEntity.setAdmissionNo(student.getAdmissionNo()); resultEntity.setName(student.getName());
        resultEntity.setRollNo(student.getRollNo()); resultEntity.setClassName(student.getClassName()); resultEntity.setExam(schedule.getExamName());
        resultEntity.setScheduleId(scheduleId); resultEntity.setMaxMarks(maximum); resultEntity.setTotal((int)Math.round(score));
        resultEntity.setPercent(percent); resultEntity.setGrade(grade); resultEntity.setResult(percent >= 40 ? "PASS" : "FAIL");
        resultEntity = examResultRepository.save(resultEntity);
        Map<String,Object> target = createExam("res_" + resultEntity.getId(), schedule.getSubject(), schedule.getExamName(), "Grade: " + grade,
                schedule.getDate(), schedule.getStartTime(), schedule.getEndTime(), schedule.getRoom(), maximum, score, "completed", schedule.getClassName());
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
        UserEntity viewer = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        boolean manages = Set.of(Role.ADMIN, Role.MASTER_ADMIN, Role.SUPER_ADMIN, Role.PRINCIPAL).contains(viewer.getRole());
        List<LeaveRequestEntity> entities = leaveRequestRepository.findAllByOrderByIdDesc();
        List<Map<String, Object>> leaves = new ArrayList<>();

        for (LeaveRequestEntity lev : entities) {
            if (!manages && !Objects.equals(viewer.getId(), lev.getUserId())) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", "lev_" + lev.getId());
            m.put("dbId", lev.getId());
            m.put("employeeName", lev.getName());
            m.put("designation", lev.getUserType() != null ? lev.getUserType() : "Faculty");
            m.put("reason", lev.getReason() != null ? lev.getReason() : "Personal Leave");
            m.put("fromDate", lev.getFromDate() != null ? lev.getFromDate() : Objects.toString(lev.getDate(), ""));
            m.put("toDate", lev.getToDate() != null ? lev.getToDate() : Objects.toString(lev.getDate(), ""));
            m.put("status", lev.getStatus() != null ? lev.getStatus().toLowerCase() : "pending");
            m.put("appliedOn", lev.getApplyDate() != null ? lev.getApplyDate() : "");
            leaves.add(m);
        }

        return ResponseEntity.ok(leaves);
    }

    @PostMapping("/leaves")
    @Operation(summary = "Apply for leave - saved directly into PostgreSQL")
    public ResponseEntity<Map<String, Object>> applyLeave(@RequestBody Map<String, Object> req) {
        UserEntity user = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        String name = user.getName();
        String reason = Objects.toString(req.get("reason"), "").trim();
        if (reason.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave reason is required");
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(Objects.toString(req.get("fromDate"), "").split("[T ]")[0]);
            to = LocalDate.parse(Objects.toString(req.get("toDate"), "").split("[T ]")[0]);
            if (to.isBefore(from)) throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a valid leave date range"); }
        String fromDate = from.toString();
        LeaveRequestEntity entity = LeaveRequestEntity.builder()
                .name(name)
                .userId(user.getId()).fromDate(fromDate).toDate(to.toString())
                .userType(user != null ? user.getRole().name() : "FACULTY")
                .leaveType(String.valueOf(req.getOrDefault("leaveType", "Casual")))
                .date(fromDate)
                .duration((java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1) + " Days")
                .status("Pending")
                .reason(reason)
                .applyDate(LocalDate.now().toString())
                .build();
        entity = leaveRequestRepository.save(entity);

        Map<String, Object> leave = createLeave("lev_" + entity.getId(), name,
                user != null ? user.getRole().name() : "Faculty",
                reason, fromDate, to.toString(), "pending", LocalDate.now().toString());

        return ResponseEntity.ok(leave);
    }

    @PatchMapping("/leaves/{id}/approve")
    @Operation(summary = "Approve leave - updates PostgreSQL")
    public ResponseEntity<Map<String, Object>> approveLeave(@PathVariable String id) {
        Long key;
        try { key = Long.valueOf(id.replace("lev_", "")); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave ID"); }
        LeaveRequestEntity leave = leaveRequestRepository.findById(key).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
        leave.setStatus("Approved");
        leaveRequestRepository.save(leave);

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "approved");
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/leaves/{id}/reject")
    @Operation(summary = "Reject leave - updates PostgreSQL")
    public ResponseEntity<Map<String, Object>> rejectLeave(@PathVariable String id) {
        Long key;
        try { key = Long.valueOf(id.replace("lev_", "")); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave ID"); }
        LeaveRequestEntity leave = leaveRequestRepository.findById(key).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
        leave.setStatus("Rejected");
        leaveRequestRepository.save(leave);

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
    // 11. SUBJECT CURRICULUM & STUDY NOTES
    // ==========================================
    @GetMapping("/subjects/{subjectId}/curriculum")
    @Operation(summary = "Get curriculum (chapters, topics, and notes count) for a subject filtered by student class")
    public ResponseEntity<Map<String, Object>> getSubjectCurriculum(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String admissionNo) {

        UserEntity currentUser = securityUtil.getCurrentUser().orElse(null);
        String studentClass = null;

        if (currentUser != null && currentUser.getRole() == Role.STUDENT) {
            StudentEntity student = studentRepository.findByEmailIgnoreCase(currentUser.getEmail()).orElse(null);
            if (student != null) {
                studentClass = student.getClassName();
            }
        } else if (admissionNo != null && !admissionNo.isBlank()) {
            StudentEntity student = studentRepository.findByAdmissionNoIgnoreCase(admissionNo.trim()).orElse(null);
            if (student != null) {
                studentClass = student.getClassName();
            }
        }

        Map<String, Object> curriculum = curriculumService.getSubjectCurriculumForStudent(subjectId, studentClass);
        return ResponseEntity.ok(curriculum);
    }

    @GetMapping("/chapters/{chapterId}/notes")
    @Operation(summary = "Get study notes and learning materials for a specific chapter")
    public ResponseEntity<Map<String, Object>> getChapterNotes(@PathVariable Long chapterId) {
        Map<String, Object> notes = curriculumService.getChapterNotesForStudent(chapterId);
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/topics/{topicId}/notes")
    @Operation(summary = "Get study notes and learning materials for a specific topic")
    public ResponseEntity<Map<String, Object>> getTopicNotes(@PathVariable Long topicId) {
        Map<String, Object> notes = curriculumService.getTopicNotesForStudent(topicId);
        return ResponseEntity.ok(notes);
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



