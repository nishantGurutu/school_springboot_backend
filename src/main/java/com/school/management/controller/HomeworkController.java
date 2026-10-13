package com.school.management.controller;

import com.school.management.entity.HomeworkEntity;
import com.school.management.repository.HomeworkRepository;
import com.school.management.util.SecurityUtil;
import com.school.management.exceptions.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/homework")
@RequiredArgsConstructor
public class HomeworkController {
    private final HomeworkRepository homeworkRepository;
    private final SecurityUtil securityUtil;

    public record CreateHomework(@NotBlank String title, @NotBlank String subject,
            @NotBlank String className, String section, String description, @NotNull LocalDate dueDate) {}

    @GetMapping
    public List<HomeworkEntity> list() { return homeworkRepository.findAllByOrderByIdDesc(); }

    @PostMapping
    public HomeworkEntity create(@Valid @RequestBody CreateHomework request) {
        var teacher = securityUtil.getCurrentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (request.dueDate().isBefore(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date cannot be in the past");
        return homeworkRepository.save(HomeworkEntity.builder().title(request.title().trim())
                .subject(request.subject().trim()).className(request.className().trim())
                .section(request.section() == null ? null : request.section().trim())
                .description(request.description()).assignedBy(teacher.getName())
                .assignedDate(LocalDate.now().toString()).dueDate(request.dueDate().toString()).build());
    }
}
