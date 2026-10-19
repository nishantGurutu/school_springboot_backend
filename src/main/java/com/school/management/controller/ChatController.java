package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import com.school.management.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/mobile/chat")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatController {
    private final SecurityUtil security;
    private final UserRepository users;
    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final TeacherRepository teachers;
    private final ChatMessageRepository messages;
    private final com.school.management.service.PortalNotificationService notifications;

    private UserEntity viewer() {
        return security.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }
    private List<StudentEntity> children(UserEntity parent) {
        Map<Long, StudentEntity> linked = new LinkedHashMap<>();
        students.findByGuardianEmailIgnoreCase(parent.getEmail()).forEach(child -> linked.put(child.getId(), child));
        guardians.findByEmailIgnoreCase(parent.getEmail()).ifPresent(guardian -> {
            if (guardian.getStudentAdmissionNo() != null) for (String admission : guardian.getStudentAdmissionNo().split(","))
                students.findByAdmissionNoIgnoreCase(admission.trim()).ifPresent(child -> linked.put(child.getId(), child));
        });
        return new ArrayList<>(linked.values());
    }
    private boolean teaches(TeacherEntity teacher, StudentEntity student) {
        if (teacher.getAssignedClasses().stream().anyMatch(c -> c.getName().equalsIgnoreCase(student.getClassName()) &&
                (c.getSection() == null || c.getSection().isBlank() || c.getSection().equalsIgnoreCase(student.getSection())))) return true;
        return teacher.getAssignedClass() != null && Arrays.stream(teacher.getAssignedClass().split(",")).anyMatch(c ->
                c.trim().equalsIgnoreCase(student.getClassName()) || c.trim().equalsIgnoreCase(student.getClassName() + " - " + student.getSection()));
    }
    private boolean canContact(UserEntity viewer, UserEntity contact) {
        if (Objects.equals(viewer.getId(), contact.getId()) || !contact.isEnabled()) return false;
        UserEntity teacherUser = viewer.getRole() == Role.TEACHER ? viewer : contact.getRole() == Role.TEACHER ? contact : null;
        if (teacherUser == null) return false;
        UserEntity family = teacherUser == viewer ? contact : viewer;
        TeacherEntity teacher = teachers.findByEmailIgnoreCase(teacherUser.getEmail()).orElse(null);
        if (teacher == null) return false;
        if (family.getRole() == Role.STUDENT) return students.findByEmailIgnoreCase(family.getEmail()).map(child -> teaches(teacher, child)).orElse(false);
        if (family.getRole() == Role.PARENT) return children(family).stream().anyMatch(child -> teaches(teacher, child));
        return false;
    }
    @GetMapping("/channels")
    public List<Map<String,Object>> channels() {
        UserEntity viewer = viewer();
        List<Map<String,Object>> result = new ArrayList<>();
        for (UserEntity contact : users.findAll()) {
            if (!canContact(viewer, contact)) continue;
            List<Map<String,Object>> history = messages.conversation(viewer.getId(), contact.getId()).stream().map(message -> {
                Map<String,Object> item = new HashMap<>();
                item.put("id", message.getId()); item.put("senderId", message.getSenderId());
                item.put("senderName", Objects.equals(message.getSenderId(), viewer.getId()) ? viewer.getName() : contact.getName());
                item.put("content", message.getContent()); item.put("timestamp", message.getSentAt().toString());
                return item;
            }).toList();
            Map<String,Object> channel = new HashMap<>();
            channel.put("id", contact.getId().toString()); channel.put("name", contact.getName());
            channel.put("avatarUrl", ""); channel.put("messages", history);
            channel.put("lastMessage", history.isEmpty() ? "" : history.get(history.size() - 1).get("content"));
            channel.put("time", history.isEmpty() ? "" : history.get(history.size() - 1).get("timestamp"));
            result.add(channel);
        }
        return result;
    }
    public record SendRequest(String text) {}
    @PostMapping("/channels/{contactId}/messages")
    @Transactional
    public Map<String,Object> send(@PathVariable Long contactId, @RequestBody SendRequest request) {
        UserEntity sender = viewer();
        UserEntity contact = users.findById(contactId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
        if (!canContact(sender, contact)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contact is not linked to your classes");
        String text = Objects.toString(request.text(), "").trim();
        if (text.isEmpty() || text.length() > 4000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must contain 1 to 4000 characters");
        ChatMessageEntity message = messages.save(ChatMessageEntity.builder().senderId(sender.getId()).recipientId(contactId).content(text).sentAt(LocalDateTime.now()).build());
        notifications.send(contactId, "New message from "+sender.getName(), text, "chat", sender.getId().toString());
        return Map.of("id", message.getId(), "status", "sent");
    }
}
