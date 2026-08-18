package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleValidatorTest {

    private static final String ACCOUNT_ID = "acc-1";

    // 2025-01-01T00:00:00Z
    private static final long NOW = 1_735_689_600_000L;

    @Mock
    private MaintenanceWindowScheduleRepository scheduleRepository;

    @Mock
    private Clock clock;

    private MaintenanceOccurrenceCalculator occurrenceCalculator;

    private MaintenanceWindowScheduleValidator validator;

    @BeforeEach
    void setUp() {
        occurrenceCalculator = new MaintenanceOccurrenceCalculator();
        validator = new MaintenanceWindowScheduleValidator(scheduleRepository, occurrenceCalculator, clock);
        lenient().when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        lenient().when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void validateAcceptsWeeklySchedule() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        validator.validate(schedule, null);
    }

    @Test
    void validateRejectsDurationBelowMinimum() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setDurationMinutes(30);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule, null));
        assertThat(exception.getMessage()).contains("durationMinutes");
    }

    @Test
    void validateRejectsInvalidTimezone() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setTimezone("Not/A/Timezone");
        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule, null));
        assertThat(exception.getMessage()).contains("timezone");
    }

    @Test
    void validateRejectsTenantScopeWithMismatchedScopeId() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setScopeId("other-account");
        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule, null));
        assertThat(exception.getMessage()).contains("scopeId must equal accountId");
    }

    @Test
    void validateRejectsOverlappingSchedulesAtSameScope() {
        MaintenanceWindowSchedule existing = weeklySchedule("existing");
        existing.setId(10L);
        existing.setStartLocalTime("10:00");
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                eq(ACCOUNT_ID), eq(MaintenanceScopeType.TENANT), eq(ACCOUNT_ID))).thenReturn(Optional.of(existing));

        MaintenanceWindowSchedule candidate = weeklySchedule("candidate");
        candidate.setStartLocalTime("10:30");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(candidate, null));
        assertThat(exception.getMessage()).contains("overlap");
    }

    @Test
    void validateRejectsSchedulesStartingWithinTwentyFourHours() {
        MaintenanceWindowSchedule existing = weeklySchedule("existing");
        existing.setId(10L);
        existing.setDayOfWeek(DayOfWeek.MONDAY);
        existing.setStartLocalTime("09:00");
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                eq(ACCOUNT_ID), eq(MaintenanceScopeType.TENANT), eq(ACCOUNT_ID))).thenReturn(Optional.of(existing));

        MaintenanceWindowSchedule candidate = weeklySchedule("candidate");
        candidate.setDayOfWeek(DayOfWeek.TUESDAY);
        candidate.setStartLocalTime("08:00");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(candidate, null));
        assertThat(exception.getMessage()).contains("24 hours");
    }

    @Test
    void validateRejectsCronWithoutOccurrenceWithinNinetyDays() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.CRON);
        schedule.setCronExpression("0 0 0 1 1 ? 2099");
        schedule.setStartLocalTime(null);
        schedule.setDayOfWeek(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule, null));
        assertThat(exception.getMessage()).contains("90 days");
    }

    private MaintenanceWindowSchedule weeklySchedule(String name) {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setAccountId(ACCOUNT_ID);
        schedule.setName(name);
        schedule.setScopeType(MaintenanceScopeType.TENANT);
        schedule.setScopeId(ACCOUNT_ID);
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(120);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        return schedule;
    }
}
