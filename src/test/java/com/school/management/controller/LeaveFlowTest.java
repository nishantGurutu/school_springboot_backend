package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.repository.LeaveRequestRepository;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveFlowTest {
    @Mock SecurityUtil securityUtil;
    @Mock LeaveRequestRepository leaveRequestRepository;
    @org.mockito.Mock com.school.management.service.PortalNotificationService portalNotifications;
    @org.mockito.Mock com.school.management.repository.UserRepository portalUsers;
    @InjectMocks MobileAppController controller;
    @Test void leavePersistsOwnerAndFullDateRange() {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().id(5L).name("Teacher").role(Role.TEACHER).build()));
        when(leaveRequestRepository.save(any())).thenAnswer(invocation -> { LeaveRequestEntity saved = invocation.getArgument(0); saved.setId(1L); return saved; });
        controller.applyLeave(Map.of("reason", "Family event", "fromDate", "2026-11-01T00:00:00", "toDate", "2026-11-03T00:00:00"));
        ArgumentCaptor<LeaveRequestEntity> saved = ArgumentCaptor.forClass(LeaveRequestEntity.class);
        verify(leaveRequestRepository).save(saved.capture());
        assertEquals(5L, saved.getValue().getUserId());
        assertEquals("2026-11-03", saved.getValue().getToDate());
        assertEquals("3 Days", saved.getValue().getDuration());
    }
    @Test void teacherOnlySeesOwnLeaveRequests() {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().id(5L).role(Role.TEACHER).build()));
        when(leaveRequestRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
                LeaveRequestEntity.builder().id(1L).userId(5L).build(), LeaveRequestEntity.builder().id(2L).userId(6L).build()));
        assertEquals(1, controller.getLeaves().getBody().size());
    }
    @Test void missingLeaveApprovalDoesNotReportSuccess() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> controller.approveLeave("lev_99")).getStatusCode().value());
        verify(leaveRequestRepository, never()).save(any());
    }
}
