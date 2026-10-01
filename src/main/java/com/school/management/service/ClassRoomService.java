package com.school.management.service;

import com.school.management.dto.ClassRoomRequest;
import com.school.management.dto.ClassRoomResponse;
import com.school.management.entity.ClassRoomEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.ClassRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassRoomService {

    private final ClassRoomRepository classRoomRepository;

    @Transactional(readOnly = true)
    public List<ClassRoomResponse> getAll() {
        return classRoomRepository.findAll().stream()
                .map(ClassRoomResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassRoomResponse getById(Long id) {
        return ClassRoomResponse.fromEntity(findByIdOrThrow(id));
    }

    public ClassRoomResponse create(ClassRoomRequest request) {
        String room = requireNonBlank(request.getRoom(), "Room is required").trim();
        if (classRoomRepository.existsByRoom(room)) {
            throw new DuplicateResourceException(
                    "Room with name '" + room + "' already exists");
        }
        String status = (request.getStatus() == null || request.getStatus().isBlank()) ? "Active" : request.getStatus().trim();
        ClassRoomEntity entity = ClassRoomEntity.builder()
                .room(room)
                .capacity(request.getCapacity() == null ? null : request.getCapacity().trim())
                .status(status)
                .build();
        return ClassRoomResponse.fromEntity(classRoomRepository.save(entity));
    }

    public ClassRoomResponse update(Long id, ClassRoomRequest request) {
        ClassRoomEntity entity = findByIdOrThrow(id);
        String room = requireNonBlank(request.getRoom(), "Room is required").trim();
        if (!entity.getRoom().equalsIgnoreCase(room) && classRoomRepository.existsByRoom(room)) {
            throw new DuplicateResourceException(
                    "Room with name '" + room + "' already exists");
        }
        entity.setRoom(room);
        entity.setCapacity(request.getCapacity() == null ? null : request.getCapacity().trim());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }
        return ClassRoomResponse.fromEntity(classRoomRepository.save(entity));
    }

    public void delete(Long id) {
        ClassRoomEntity entity = findByIdOrThrow(id);
        classRoomRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private ClassRoomEntity findByIdOrThrow(Long id) {
        return classRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }
}
