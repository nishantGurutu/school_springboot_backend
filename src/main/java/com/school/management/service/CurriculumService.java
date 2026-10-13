package com.school.management.service;

import com.school.management.entity.*;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Slf4j
@Transactional
public class CurriculumService {

    private final SubjectRepository subjectRepository;
    private final SubjectChapterRepository chapterRepository;
    private final SubjectTopicRepository topicRepository;
    private final TopicNoteRepository noteRepository;
    private final TeacherRepository teacherRepository;
    private final FileStorageService fileStorageService;
    private final StudentRepository studentRepository;
    private final com.school.management.util.SecurityUtil securityUtil;

    public CurriculumService(SubjectRepository subjectRepository,
                             SubjectChapterRepository chapterRepository,
                             SubjectTopicRepository topicRepository,
                             TopicNoteRepository noteRepository,
                             TeacherRepository teacherRepository,
                             FileStorageService fileStorageService,
                             StudentRepository studentRepository,
                             com.school.management.util.SecurityUtil securityUtil) {
        this.subjectRepository = subjectRepository;
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
        this.noteRepository = noteRepository;
        this.teacherRepository = teacherRepository;
        this.fileStorageService = fileStorageService;
        this.studentRepository = studentRepository;
        this.securityUtil = securityUtil;
    }

    private StudentEntity currentStudent() {
        var user = securityUtil.getCurrentUser().orElseThrow(() -> new com.school.management.exceptions.UnauthorizedException("Not authenticated"));
        if (user.getRole() != com.school.management.domain.user.Role.STUDENT) return null;
        return studentRepository.findByEmailIgnoreCase(user.getEmail()).orElseThrow(() -> new ResourceNotFoundException("Student profile not linked"));
    }

    private boolean classMatches(String curriculumClass, String studentClass) {
        return curriculumClass == null || curriculumClass.isBlank() || curriculumClass.equalsIgnoreCase("All")
                || (studentClass != null && curriculumClass.trim().equalsIgnoreCase(studentClass.trim()));
    }

    private void checkSubject(StudentEntity student, SubjectEntity subject) {
        if (student == null) return;
        boolean assigned = student.getSubjectIds() != null && !student.getSubjectIds().isBlank()
                ? Arrays.stream(student.getSubjectIds().split(",")).anyMatch(id -> id.trim().equals(subject.getId().toString()))
                : classMatches(subject.getClassName(), student.getClassName());
        if (!assigned) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Subject is not assigned to you");
    }

    private void checkChapter(StudentEntity student, SubjectChapterEntity chapter) {
        if (student == null) return;
        checkSubject(student, chapter.getSubject());
        if (!classMatches(chapter.getClassName(), student.getClassName()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Chapter is not assigned to your class");
    }

    // ==========================================
    // TEACHER CURRICULUM MANAGEMENT
    // ==========================================

    @Transactional(readOnly = true)
    public List<SubjectEntity> getTeacherSubjects(Long teacherId, String teacherEmail) {
        return getTeacherSubjects(teacherId, teacherEmail, null);
    }

    @Transactional(readOnly = true)
    public List<SubjectEntity> getTeacherSubjects(Long teacherId, String teacherEmail, String className) {
        // 1. If className specified, check if subjects specifically match this class
        if (className != null && !className.isBlank() && !"all".equalsIgnoreCase(className.trim())) {
            List<SubjectEntity> classSubjects = subjectRepository.findByClassNameIgnoreCaseOrClassNameIsNull(className.trim());
            if (!classSubjects.isEmpty()) {
                return classSubjects;
            }
        }

        TeacherEntity teacher = null;
        if (teacherId != null) {
            teacher = teacherRepository.findById(teacherId).orElse(null);
        }
        if (teacher == null && teacherEmail != null && !teacherEmail.isBlank()) {
            teacher = teacherRepository.findByEmailIgnoreCase(teacherEmail.trim()).orElse(null);
        }

        if (teacher != null) {
            if (teacher.getSubjectSpecializations() != null && !teacher.getSubjectSpecializations().isEmpty()) {
                return new ArrayList<>(teacher.getSubjectSpecializations());
            }
            if (teacher.getSubjectIds() != null && !teacher.getSubjectIds().isBlank()) {
                try {
                    List<Long> ids = Arrays.stream(teacher.getSubjectIds().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Long::parseLong)
                            .toList();
                    if (!ids.isEmpty()) {
                        return subjectRepository.findAllById(ids);
                    }
                } catch (Exception ex) {
                    log.warn("Could not parse teacher subject IDs: {}", ex.getMessage());
                }
            }
            if (teacher.getSubject() != null && !teacher.getSubject().isBlank()) {
                List<SubjectEntity> byName = subjectRepository.findByNameIgnoreCase(teacher.getSubject().trim());
                if (!byName.isEmpty()) return byName;
            }
        }
        // Fallback: Return all active subjects so teacher can manage curriculum
        return subjectRepository.findAll();
    }

    public SubjectEntity createSubject(String name, String code, String className, String status) {
        String cleanName = (name != null && !name.isBlank()) ? name.trim() : "New Subject";
        String cleanClass = (className != null && !className.isBlank() && !"all".equalsIgnoreCase(className)) ? className.trim() : "Class 10";
        String cleanCode = (code != null && !code.isBlank()) ? code.trim().toUpperCase() : null;

        if (cleanCode == null) {
            String prefix = cleanName.length() >= 3 ? cleanName.substring(0, 3).toUpperCase() : cleanName.toUpperCase();
            String classNum = cleanClass.replaceAll("[^0-9]", "");
            if (classNum.isEmpty()) classNum = "01";
            cleanCode = prefix + "-" + classNum;
        }

        // Ensure uniqueness
        if (subjectRepository.existsByCode(cleanCode)) {
            cleanCode = cleanCode + "-" + (System.currentTimeMillis() % 1000);
        }

        SubjectEntity entity = SubjectEntity.builder()
                .name(cleanName)
                .code(cleanCode)
                .className(cleanClass)
                .status(status != null && !status.isBlank() ? status.trim() : "Active")
                .build();
        return subjectRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChaptersWithTopics(Long subjectId, String className) {
        List<SubjectChapterEntity> chapters;
        boolean hasClass = className != null && !className.isBlank() && !"all".equalsIgnoreCase(className.trim());

        if (subjectId != null) {
            if (hasClass) {
                chapters = chapterRepository.findBySubjectIdAndClassNameFlexible(subjectId, className.trim());
            } else {
                chapters = chapterRepository.findBySubjectIdOrderByDisplayOrderAsc(subjectId);
            }
        } else {
            if (hasClass) {
                chapters = chapterRepository.findByClassNameFlexible(className.trim());
            } else {
                chapters = chapterRepository.findAll();
            }
        }

        return chapters.stream().map(this::mapChapterToDto).toList();
    }

    public Map<String, Object> mapChapterToDto(SubjectChapterEntity ch) {
        Map<String, Object> chMap = new HashMap<>();
        chMap.put("id", ch.getId());
        chMap.put("chapterNumber", ch.getChapterNumber());
        chMap.put("chapterTitle", ch.getChapterTitle());
        chMap.put("className", ch.getClassName());
        chMap.put("description", ch.getDescription());
        chMap.put("displayOrder", ch.getDisplayOrder());

        if (ch.getSubject() != null) {
            chMap.put("subjectId", ch.getSubject().getId());
            chMap.put("subjectName", ch.getSubject().getName());
            chMap.put("subjectCode", ch.getSubject().getCode());
        }

        List<TopicNoteEntity> chapterNotes = noteRepository.findByChapterIdOrderByCreatedAtDesc(ch.getId());
        chMap.put("notes", chapterNotes.stream().map(this::mapNoteToDto).toList());
        chMap.put("notesCount", chapterNotes.size());

        List<SubjectTopicEntity> topics = topicRepository.findByChapterIdOrderByDisplayOrderAsc(ch.getId());
        List<Map<String, Object>> topicMaps = new ArrayList<>();
        for (SubjectTopicEntity t : topics) {
            Map<String, Object> tm = new HashMap<>();
            tm.put("id", t.getId());
            tm.put("title", t.getTitle());
            tm.put("description", t.getDescription());
            tm.put("difficulty", t.getDifficulty());
            tm.put("estimatedMinutes", t.getEstimatedMinutes());
            tm.put("displayOrder", t.getDisplayOrder());
            tm.put("status", t.getStatus());

            List<TopicNoteEntity> notes = noteRepository.findByTopicIdOrderByCreatedAtDesc(t.getId());
            tm.put("notesCount", notes.size());
            tm.put("notes", notes.stream().map(this::mapNoteToDto).toList());
            topicMaps.add(tm);
        }
        chMap.put("topics", topicMaps);
        return chMap;
    }

    public SubjectChapterEntity createChapter(Long subjectId, String className, String chapterNumber, String title, String desc, Integer order) {
        SubjectEntity subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        String effectiveClass = (className != null && !className.isBlank())
                ? className.trim()
                : (subject.getClassName() != null && !subject.getClassName().isBlank() ? subject.getClassName().trim() : "All");

        SubjectChapterEntity entity = SubjectChapterEntity.builder()
                .subject(subject)
                .className(effectiveClass)
                .chapterNumber(chapterNumber != null && !chapterNumber.isBlank() ? chapterNumber.trim() : "Chapter 1")
                .chapterTitle(title.trim())
                .description(desc)
                .displayOrder(order != null ? order : 1)
                .build();
        return chapterRepository.save(entity);
    }

    public SubjectChapterEntity updateChapter(Long id, String chapterNumber, String title, String desc, Integer order) {
        return updateChapter(id, chapterNumber, title, desc, order, null, null);
    }

    public SubjectChapterEntity updateChapter(Long id, String chapterNumber, String title, String desc, Integer order, String className, Long subjectId) {
        SubjectChapterEntity entity = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found: " + id));
        if (chapterNumber != null && !chapterNumber.isBlank()) entity.setChapterNumber(chapterNumber.trim());
        if (title != null && !title.isBlank()) entity.setChapterTitle(title.trim());
        if (desc != null) entity.setDescription(desc);
        if (order != null) entity.setDisplayOrder(order);
        if (className != null && !className.isBlank()) entity.setClassName(className.trim());
        if (subjectId != null) {
            SubjectEntity subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
            entity.setSubject(subject);
        }
        return chapterRepository.save(entity);
    }

    public void deleteChapter(Long id) {
        chapterRepository.deleteById(id);
    }

    public SubjectTopicEntity createTopic(Long chapterId, String title, String desc, String difficulty, Integer minutes, Integer order) {
        SubjectChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found: " + chapterId));

        SubjectTopicEntity entity = SubjectTopicEntity.builder()
                .chapter(chapter)
                .subject(chapter.getSubject())
                .className(chapter.getClassName())
                .title(title.trim())
                .description(desc)
                .difficulty(difficulty != null ? difficulty.trim() : "Medium")
                .estimatedMinutes(minutes != null ? minutes : 30)
                .displayOrder(order != null ? order : 1)
                .status("Active")
                .build();
        return topicRepository.save(entity);
    }

    public SubjectTopicEntity updateTopic(Long id, String title, String desc, String difficulty, Integer minutes, Integer order, String status) {
        SubjectTopicEntity entity = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
        if (title != null) entity.setTitle(title.trim());
        if (desc != null) entity.setDescription(desc);
        if (difficulty != null) entity.setDifficulty(difficulty.trim());
        if (minutes != null) entity.setEstimatedMinutes(minutes);
        if (order != null) entity.setDisplayOrder(order);
        if (status != null) entity.setStatus(status.trim());
        return topicRepository.save(entity);
    }

    public void deleteTopic(Long id) {
        topicRepository.deleteById(id);
    }

    public TopicNoteEntity addNote(Long chapterId, Long topicId, String title, String description, String contentHtml,
                                  MultipartFile file, String directUrl, Boolean isDownloadable,
                                  Long teacherId, String teacherName) {

        SubjectChapterEntity chapter = null;
        SubjectTopicEntity topic = null;
        SubjectEntity subject = null;
        String className = null;

        if (chapterId != null) {
            chapter = chapterRepository.findById(chapterId).orElse(null);
            if (chapter != null) {
                subject = chapter.getSubject();
                className = chapter.getClassName();
            }
        }

        if (topicId != null) {
            topic = topicRepository.findById(topicId).orElse(null);
            if (topic != null) {
                if (chapter == null) {
                    chapter = topic.getChapter();
                }
                if (subject == null) {
                    subject = topic.getSubject();
                }
                if (className == null) {
                    className = topic.getClassName();
                }
            }
        }

        if (chapter == null && topic == null) {
            throw new ResourceNotFoundException("Either chapterId or topicId must be provided for note");
        }

        String fileName = null;
        String fileUrl = directUrl;
        String fileType = "PDF";
        long sizeBytes = 0L;
        String formattedSize = "1.0 MB";

        if (file != null && !file.isEmpty()) {
            FileStorageService.UploadResult uploaded = fileStorageService.storeFile(file, "notes");
            fileName = uploaded.originalFileName();
            fileUrl = uploaded.downloadUrl();
            fileType = uploaded.fileType();
            sizeBytes = uploaded.fileSizeBytes();
            formattedSize = uploaded.fileSizeFormatted();
        } else if (fileUrl != null && !fileUrl.isBlank()) {
            fileName = title != null ? (title.trim() + ".pdf") : "document.pdf";
            if (fileUrl.toLowerCase().endsWith(".pdf")) fileType = "PDF";
            else if (fileUrl.toLowerCase().endsWith(".doc") || fileUrl.toLowerCase().endsWith(".docx")) fileType = "DOC";
            else if (fileUrl.toLowerCase().endsWith(".png") || fileUrl.toLowerCase().endsWith(".jpg")) fileType = "IMAGE";
        }

        TopicNoteEntity note = TopicNoteEntity.builder()
                .chapter(chapter)
                .topic(topic)
                .subject(subject)
                .className(className)
                .title(title.trim())
                .description(description)
                .contentHtml(contentHtml)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileType(fileType)
                .fileSizeBytes(sizeBytes)
                .fileSizeFormatted(formattedSize)
                .isDownloadable(isDownloadable != null ? isDownloadable : true)
                .isPreviewable(true)
                .uploadedByTeacherId(teacherId)
                .uploadedByTeacherName(teacherName != null ? teacherName : "Teacher")
                .build();

        return noteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        noteRepository.deleteById(noteId);
    }

    // ==========================================
    // STUDENT / MOBILE APP CONSUMPTION
    // ==========================================

    @Transactional(readOnly = true)
    public Map<String, Object> getSubjectCurriculumForStudent(Long subjectId, String studentClassName) {
        SubjectEntity subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));

        List<SubjectChapterEntity> chapters = chapterRepository.findBySubjectIdOrderByDisplayOrderAsc(subjectId);
        
        StudentEntity student = currentStudent();
        checkSubject(student, subject);
        String effectiveClass = student != null ? student.getClassName() : studentClassName;
        if (effectiveClass != null && !effectiveClass.isBlank()) {
            chapters = chapters.stream().filter(chapter -> classMatches(chapter.getClassName(), effectiveClass)).toList();
        }
        List<Map<String, Object>> chapterList = new ArrayList<>();
        int totalTopics = 0;
        int totalNotes = 0;

        for (SubjectChapterEntity ch : chapters) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("chapterId", ch.getId());
            cm.put("chapterNumber", ch.getChapterNumber());
            cm.put("chapterTitle", ch.getChapterTitle());
            cm.put("className", ch.getClassName());
            cm.put("description", ch.getDescription());

            // Direct Chapter Notes ("notes chapter ki hogi")
            List<TopicNoteEntity> chapterNotes = noteRepository.findByChapterIdOrderByCreatedAtDesc(ch.getId());
            cm.put("notes", chapterNotes.stream().map(this::mapNoteToDto).toList());
            cm.put("notesCount", chapterNotes.size());
            cm.put("hasNotes", !chapterNotes.isEmpty());
            totalNotes += chapterNotes.size();

            List<SubjectTopicEntity> topics = topicRepository.findByChapterIdOrderByDisplayOrderAsc(ch.getId());
            List<Map<String, Object>> topicList = new ArrayList<>();

            for (SubjectTopicEntity t : topics) {
                totalTopics++;
                Map<String, Object> tm = new HashMap<>();
                tm.put("topicId", t.getId());
                tm.put("title", t.getTitle());
                tm.put("description", t.getDescription());
                tm.put("difficulty", t.getDifficulty());
                tm.put("estimatedMinutes", t.getEstimatedMinutes());

                List<TopicNoteEntity> tNotes = noteRepository.findByTopicIdOrderByCreatedAtDesc(t.getId());
                tm.put("notes", tNotes.stream().map(this::mapNoteToDto).toList());
                tm.put("notesCount", tNotes.size());
                tm.put("hasNotes", !tNotes.isEmpty());
                totalNotes += tNotes.size();
                topicList.add(tm);
            }
            cm.put("topics", topicList);
            chapterList.add(cm);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("subjectId", subject.getId());
        result.put("subjectName", subject.getName());
        result.put("subjectCode", subject.getCode());
        result.put("totalChapters", chapterList.size());
        result.put("totalTopics", totalTopics);
        result.put("totalNotes", totalNotes);
        result.put("chapters", chapterList);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChapterNotesForStudent(Long chapterId) {
        SubjectChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found: " + chapterId));

        checkChapter(currentStudent(), chapter);
        List<TopicNoteEntity> notes = noteRepository.findByChapterIdOrderByCreatedAtDesc(chapterId);

        Map<String, Object> response = new HashMap<>();
        response.put("chapterId", chapter.getId());
        response.put("chapterNumber", chapter.getChapterNumber());
        response.put("chapterTitle", chapter.getChapterTitle());
        response.put("className", chapter.getClassName());
        response.put("description", chapter.getDescription());
        if (chapter.getSubject() != null) {
            response.put("subjectId", chapter.getSubject().getId());
            response.put("subjectName", chapter.getSubject().getName());
            response.put("subjectCode", chapter.getSubject().getCode());
        }
        response.put("notes", notes.stream().map(this::mapNoteToDto).toList());
        response.put("totalNotes", notes.size());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTopicNotesForStudent(Long topicId) {
        SubjectTopicEntity topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        StudentEntity student = currentStudent();
        if (topic.getChapter() != null) checkChapter(student, topic.getChapter());
        else {
            checkSubject(student, topic.getSubject());
            if (student != null && !classMatches(topic.getClassName(), student.getClassName()))
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Topic is not assigned to your class");
        }
        List<TopicNoteEntity> notes = noteRepository.findByTopicIdOrderByCreatedAtDesc(topicId);

        Map<String, Object> response = new HashMap<>();
        response.put("topicId", topic.getId());
        response.put("topicTitle", topic.getTitle());
        response.put("topicDescription", topic.getDescription());
        response.put("difficulty", topic.getDifficulty());
        response.put("estimatedMinutes", topic.getEstimatedMinutes());
        response.put("chapterTitle", topic.getChapter() != null ? topic.getChapter().getChapterTitle() : "");
        response.put("chapterNumber", topic.getChapter() != null ? topic.getChapter().getChapterNumber() : "");
        response.put("subjectName", topic.getSubject() != null ? topic.getSubject().getName() : "");
        response.put("notes", notes.stream().map(this::mapNoteToDto).toList());
        return response;
    }

    private Map<String, Object> mapNoteToDto(TopicNoteEntity note) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", note.getId());
        m.put("title", note.getTitle());
        m.put("description", note.getDescription());
        m.put("contentHtml", note.getContentHtml());
        m.put("fileName", note.getFileName());
        m.put("fileUrl", note.getFileUrl());
        m.put("fileType", note.getFileType());
        m.put("fileSizeBytes", note.getFileSizeBytes());
        m.put("fileSizeFormatted", note.getFileSizeFormatted());
        m.put("isDownloadable", note.getIsDownloadable());
        m.put("isPreviewable", note.getIsPreviewable());
        m.put("teacherName", note.getUploadedByTeacherName());
        m.put("uploadedDate", note.getCreatedAt() != null ? note.getCreatedAt().toLocalDate().toString() : "Recent");
        return m;
    }
}

