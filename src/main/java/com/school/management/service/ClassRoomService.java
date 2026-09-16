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

    public ClassRoomResponse create(ClassRoomRequest request) {
        String room = requireNonBlank(request.getRoom(), "Room is required");
        if (classRoomRepository.existsByRoom(room.trim())) {
            throw new DuplicateResourceException(
                    "Room with name '" + room + "' already exists");
        }
        ClassRoomEntity entity = ClassRoomEntity.builder()
                .room(room.trim())
                .capacity(request.getCapacity() == null ? null : request.getCapacity().trim())
                .build();
        return ClassRoomResponse.fromEntity(classRoomRepository.save(entity));
    }

    public ClassRoomResponse update(Long id, ClassRoomRequest request) {
        ClassRoomEntity entity = findByIdOrThrow(id);
        String room = requireNonBlank(request.getRoom(), "Room is required");
        if (!entity.getRoom().equals(room.trim()) && classRoomRepository.existsByRoom(room.trim())) {
            throw new DuplicateResourceException(
                    "Room with name '" + room + "' already exists");
        }
        entity.setRoom(room.trim());
        entity.setCapacity(request.getCapacity() == null ? null : request.getCapacity().trim());
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
