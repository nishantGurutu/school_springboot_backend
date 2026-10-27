package com.school.management.controller;
import com.school.management.domain.user.Role;
import com.school.management.dto.HolidayResponse;
import com.school.management.entity.UserEntity;
import com.school.management.service.HolidayService;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MobileHolidayTargetTest {
    @Mock HolidayService holidayService;
    @Mock SecurityUtil securityUtil;
    @InjectMocks MobileAppController controller;

    private void setup(Role role) {
        when(securityUtil.getCurrentUser()).thenReturn(role == null ? Optional.empty() : Optional.of(UserEntity.builder().role(role).build()));
        when(holidayService.getAll()).thenReturn(List.of(
            HolidayResponse.builder().id(1L).title("Student holiday").date("2026-11-02").target("STUDENTS").build(),
            HolidayResponse.builder().id(2L).title("Staff holiday").date("2026-11-03").target("STAFF").build(),
            HolidayResponse.builder().id(3L).title("Everyone").date("2026-11-01").target("ALL").build()));
    }
    @Test void studentsSeeAllAndStudentHolidays() {
        setup(Role.STUDENT);
        assertEquals(List.of(3L, 1L), controller.getMobileHolidays().getBody().stream().map(HolidayResponse::getId).toList());
    }
    @Test void teachersSeeAllAndStaffHolidays() {
        setup(Role.TEACHER);
        assertEquals(List.of(3L, 2L), controller.getMobileHolidays().getBody().stream().map(HolidayResponse::getId).toList());
    }
    @Test void anonymousViewContainsOnlyAllHolidays() {
        setup(null);
        assertEquals(List.of(3L), controller.getMobileHolidays().getBody().stream().map(HolidayResponse::getId).toList());
    }
}
