package com.school.management.controller;

import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController @RequestMapping("/api/student-reports") @RequiredArgsConstructor
@Transactional(readOnly=true)
public class StudentReportController {
    private final StudentReportRepository reports;
    private final StudentRepository students;
    private final PortalAccessService access;
    private final FileStorageService files;

    @GetMapping("/students") public List<Map<String,Object>> eligible(@RequestParam String className,
            @RequestParam(required=false) String section) {
        return access.eligible(className,section).stream().map(s -> Map.<String,Object>of(
                "id",s.getId(),"name",s.getName(),"admissionNo",s.getAdmissionNo())).toList();
    }

    @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false) String admissionNo) {
        if (!access.manager() || admissionNo != null && !admissionNo.isBlank()) {
            StudentEntity student=access.student(admissionNo);
            return reports.findByStudentIdOrderByIdDesc(student.getId()).stream().map(r -> dto(r,student)).toList();
        }
        return reports.findAllByOrderByIdDesc().stream().filter(r -> students.findById(r.getStudentId())
                .map(s -> access.teaches(s.getClassName(),s.getSection())).orElse(false))
                .map(r -> dto(r,students.findById(r.getStudentId()).orElseThrow())).toList();
    }

    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional
    public Map<String,Object> publish(@RequestParam String admissionNo,@RequestParam String title,
            @RequestParam(required=false) String term,@RequestParam(required=false) String description,
            @RequestParam MultipartFile file) throws IOException {
        access.requireManager();
        StudentEntity student=access.student(admissionNo);
        if(title.isBlank() || title.trim().length()>255 || term!=null && term.length()>128 || description!=null && description.length()>2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a title up to 255 characters, term up to 128 and description up to 2000");
        if(file.isEmpty() || file.getSize()>20L*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a PDF up to 20 MB");
        try(var input=file.getInputStream()) {
            if(!Arrays.equals(input.readNBytes(5),"%PDF-".getBytes(StandardCharsets.US_ASCII)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Report must be a PDF file");
        }
        var stored=files.storeFile(file,"reports");
        var report=reports.save(StudentReportEntity.builder().studentId(student.getId()).title(title.trim())
                .term(term).description(description).storedFileName(stored.storedFileName())
                .fileName(stored.originalFileName()).fileSizeBytes(stored.fileSizeBytes())
                .publishedBy(access.actor().getId()).build());
        return dto(report,student);
    }

    private StudentReportEntity accessible(Long id) {
        var report=reports.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Report not found"));
        var student=students.findById(report.getStudentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"));
        if (!access.student(student.getAdmissionNo()).getId().equals(report.getStudentId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Report belongs to another student");
        return report;
    }

    @GetMapping("/{id}/pdf") public ResponseEntity<Resource> pdf(@PathVariable Long id,
            @RequestParam(defaultValue="false") boolean attachment) {
        var report=accessible(id);
        var disposition=(attachment?ContentDisposition.attachment():ContentDisposition.inline())
                .filename(report.getFileName(),StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString())
                .body(files.loadFileAsResource("reports/"+report.getStoredFileName()));
    }

    @DeleteMapping("/{id}") @Transactional public void delete(@PathVariable Long id) {
        access.requireManager();
        reports.delete(accessible(id));
    }

    private Map<String,Object> dto(StudentReportEntity r,StudentEntity s) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",r.getId());result.put("title",r.getTitle());result.put("term",r.getTerm());
        result.put("description",r.getDescription());result.put("studentName",s.getName());
        result.put("admissionNo",s.getAdmissionNo());result.put("className",s.getClassName());result.put("section",s.getSection());
        result.put("fileName",r.getFileName());result.put("fileSizeBytes",r.getFileSizeBytes());result.put("createdAt",r.getCreatedAt());
        result.put("fileUrl","/api/student-reports/"+r.getId()+"/pdf");return result;
    }
}
