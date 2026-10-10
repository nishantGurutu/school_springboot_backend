package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.service.CurriculumService;
import com.school.management.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/curriculum")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Curriculum & Notes", description = "Teacher-only endpoints for managing Subject Chapters, Topics, and Study Notes")
public class TeacherCurriculumController {

    private final CurriculumService curriculumService;
    private final SecurityUtil securityUtil;

    // 1. Get subjects assigned to the logged-in teacher (optionally filtered by class)
    @GetMapping("/subjects")
    @Operation(summary = "Get subjects assigned to the logged in teacher or class")
    public ResponseEntity<List<Map<String, Object>>> getTeacherSubjects(
            @RequestParam(required = false) String className) {
        UserEntity currentUser = securityUtil.getCurrentUser().orElse(null);
        String email = currentUser != null ? currentUser.getEmail() : null;

        List<SubjectEntity> subjects = curriculumService.getTeacherSubjects(null, email, className);
        List<Map<String, Object>> response = subjects.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("code", s.getCode());
            m.put("className", s.getClassName());
            m.put("status", s.getStatus());
            return m;
        }).toList();

        return ResponseEntity.ok(response);
    }

    // 1.1 Create a new subject for a class directly from Teacher Curriculum
    @PostMapping("/subjects")
    @Operation(summary = "Create a subject for a class")
    public ResponseEntity<Map<String, Object>> createSubject(@RequestBody Map<String, Object> req) {
        String name = (String) req.get("name");
        String code = (String) req.get("code");
        String className = (String) req.get("className");
        String status = (String) req.get("status");

        SubjectEntity saved = curriculumService.createSubject(name, code, className, status);
        Map<String, Object> m = new HashMap<>();
        m.put("id", saved.getId());
        m.put("name", saved.getName());
        m.put("code", saved.getCode());
        m.put("className", saved.getClassName());
        m.put("status", saved.getStatus());
        return ResponseEntity.ok(m);
    }

    // 2. Get chapters & topics tree for a selected subject and class
    @GetMapping("/chapters")
    @Operation(summary = "Get chapters and topics for a subject")
    public ResponseEntity<List<Map<String, Object>>> getChapters(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String className) {
        return ResponseEntity.ok(curriculumService.getChaptersWithTopics(subjectId, className));
    }

    // 3. Create chapter
    @PostMapping("/chapters")
    @Operation(summary = "Create a new chapter under a subject")
    public ResponseEntity<SubjectChapterEntity> createChapter(@RequestBody Map<String, Object> req) {
        Long subjectId = Long.valueOf(req.get("subjectId").toString());
        String className = (String) req.get("className");
        String chapterNumber = (String) req.get("chapterNumber");
        String title = (String) req.get("chapterTitle");
        String desc = (String) req.get("description");
        Integer order = req.get("displayOrder") != null ? Integer.valueOf(req.get("displayOrder").toString()) : 1;

        SubjectChapterEntity saved = curriculumService.createChapter(subjectId, className, chapterNumber, title, desc, order);
        return ResponseEntity.ok(saved);
    }

    // 4. Update chapter
    @PutMapping("/chapters/{id}")
    @Operation(summary = "Update chapter details")
    public ResponseEntity<SubjectChapterEntity> updateChapter(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String chapterNumber = (String) req.get("chapterNumber");
        String title = (String) req.get("chapterTitle");
        String desc = (String) req.get("description");
        Integer order = req.get("displayOrder") != null ? Integer.valueOf(req.get("displayOrder").toString()) : 1;

        return ResponseEntity.ok(curriculumService.updateChapter(id, chapterNumber, title, desc, order));
    }

    // 5. Delete chapter
    @DeleteMapping("/chapters/{id}")
    @Operation(summary = "Delete chapter and all associated topics")
    public ResponseEntity<Map<String, Object>> deleteChapter(@PathVariable Long id) {
        curriculumService.deleteChapter(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Chapter deleted"));
    }

    // 6. Create topic
    @PostMapping("/topics")
    @Operation(summary = "Create a topic under a chapter")
    public ResponseEntity<SubjectTopicEntity> createTopic(@RequestBody Map<String, Object> req) {
        Long chapterId = Long.valueOf(req.get("chapterId").toString());
        String title = (String) req.get("title");
        String desc = (String) req.get("description");
        String difficulty = req.get("difficulty") != null ? req.get("difficulty").toString() : "Medium";
        Integer minutes = req.get("estimatedMinutes") != null ? Integer.valueOf(req.get("estimatedMinutes").toString()) : 30;
        Integer order = req.get("displayOrder") != null ? Integer.valueOf(req.get("displayOrder").toString()) : 1;

        return ResponseEntity.ok(curriculumService.createTopic(chapterId, title, desc, difficulty, minutes, order));
    }

    // 7. Update topic
    @PutMapping("/topics/{id}")
    @Operation(summary = "Update topic")
    public ResponseEntity<SubjectTopicEntity> updateTopic(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String title = (String) req.get("title");
        String desc = (String) req.get("description");
        String difficulty = (String) req.get("difficulty");
        Integer minutes = req.get("estimatedMinutes") != null ? Integer.valueOf(req.get("estimatedMinutes").toString()) : null;
        Integer order = req.get("displayOrder") != null ? Integer.valueOf(req.get("displayOrder").toString()) : null;
        String status = (String) req.get("status");

        return ResponseEntity.ok(curriculumService.updateTopic(id, title, desc, difficulty, minutes, order, status));
    }

    // 8. Delete topic
    @DeleteMapping("/topics/{id}")
    @Operation(summary = "Delete topic and its notes")
    public ResponseEntity<Map<String, Object>> deleteTopic(@PathVariable Long id) {
        curriculumService.deleteTopic(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Topic deleted"));
    }

    // 9. Add note with optional file upload (Multipart) - Attached to Chapter or Topic
    @PostMapping(value = "/notes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and attach note to a Chapter or Topic (with file)")
    public ResponseEntity<TopicNoteEntity> addNoteWithFile(
            @RequestParam(value = "chapterId", required = false) Long chapterId,
            @RequestParam(value = "topicId", required = false) Long topicId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "contentHtml", required = false) String contentHtml,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "directUrl", required = false) String directUrl,
            @RequestParam(value = "isDownloadable", required = false, defaultValue = "true") Boolean isDownloadable) {

        UserEntity currentUser = securityUtil.getCurrentUser().orElse(null);
        String teacherName = currentUser != null ? currentUser.getName() : "Teacher";
        Long teacherId = currentUser != null ? currentUser.getId() : null;

        TopicNoteEntity note = curriculumService.addNote(
                chapterId, topicId, title, description, contentHtml, file, directUrl, isDownloadable, teacherId, teacherName
        );
        return ResponseEntity.ok(note);
    }

    // 10. Add note as JSON (e.g. text/rich notes or URL reference)
    @PostMapping("/notes/json")
    @Operation(summary = "Add note to a Chapter or Topic without uploading binary file")
    public ResponseEntity<TopicNoteEntity> addNoteJson(@RequestBody Map<String, Object> req) {
        Long chapterId = req.get("chapterId") != null && !req.get("chapterId").toString().isBlank()
                ? Long.valueOf(req.get("chapterId").toString()) : null;
        Long topicId = req.get("topicId") != null && !req.get("topicId").toString().isBlank()
                ? Long.valueOf(req.get("topicId").toString()) : null;
        String title = (String) req.get("title");
        String desc = (String) req.get("description");
        String contentHtml = (String) req.get("contentHtml");
        String directUrl = (String) req.get("fileUrl");
        Boolean isDownloadable = req.get("isDownloadable") != null ? Boolean.valueOf(req.get("isDownloadable").toString()) : true;

        UserEntity currentUser = securityUtil.getCurrentUser().orElse(null);
        String teacherName = currentUser != null ? currentUser.getName() : "Teacher";
        Long teacherId = currentUser != null ? currentUser.getId() : null;

        TopicNoteEntity note = curriculumService.addNote(
                chapterId, topicId, title, desc, contentHtml, null, directUrl, isDownloadable, teacherId, teacherName
        );
        return ResponseEntity.ok(note);
    }

    // 11. Delete note
    @DeleteMapping("/notes/{id}")
    @Operation(summary = "Delete note")
    public ResponseEntity<Map<String, Object>> deleteNote(@PathVariable Long id) {
        curriculumService.deleteNote(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Note deleted"));
    }
}
