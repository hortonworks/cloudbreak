package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSkipResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowSkipRepository;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowSkipServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:acc-1:user:1";

    private static final long NOW = Instant.parse("2025-01-06T10:30:00Z").toEpochMilli();

    private static final long NEXT_WINDOW_START = Instant.parse("2025-01-13T09:00:00Z").toEpochMilli();

    @Mock
    private MaintenanceWindowScheduleService scheduleService;

    @Mock
    private MaintenanceWindowSkipRepository skipRepository;

    @Mock
    private Clock clock;

    private MaintenanceWindowScheduleConverter scheduleConverter;

    private MaintenanceWindowSkipService skipService;

    @BeforeEach
    void setUp() {
        scheduleConverter = new MaintenanceWindowScheduleConverter(new MaintenanceOccurrenceCalculator(), clock);
        skipService = new MaintenanceWindowSkipService(
                scheduleService, skipRepository, scheduleConverter, new MaintenanceOccurrenceCalculator(), clock);
    }

    @Test
    void skipNextWindowMapsDuplicateKeyToConflict() {
        MaintenanceWindowSchedule schedule = schedule();
        when(scheduleService.findRequired(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(schedule);
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(skipRepository.findByMaintenanceWindowScheduleIdAndWindowStart(schedule.getId(), NEXT_WINDOW_START))
                .thenReturn(Optional.empty());
        when(skipRepository.save(any(MaintenanceWindowSkip.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> skipService.skipNextWindow(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, null, USER_CRN));

        assertThat(exception.getMessage()).contains("already skipped");
    }

    @Test
    void skipNextWindowRejectsExistingSkip() {
        MaintenanceWindowSchedule schedule = schedule();
        MaintenanceWindowSkip existing = new MaintenanceWindowSkip();
        when(scheduleService.findRequired(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(schedule);
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(skipRepository.findByMaintenanceWindowScheduleIdAndWindowStart(schedule.getId(), NEXT_WINDOW_START))
                .thenReturn(Optional.of(existing));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> skipService.skipNextWindow(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, null, USER_CRN));

        assertThat(exception.getMessage()).contains("already skipped");
    }

    @Test
    void skipNextWindowRejectsInProgressWindow() {
        MaintenanceWindowSchedule schedule = schedule();
        long duringWindow = Instant.parse("2025-01-06T09:30:00Z").toEpochMilli();
        when(scheduleService.findRequired(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(schedule);
        when(clock.getCurrentTimeMillis()).thenReturn(duringWindow);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> skipService.skipNextWindow(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, null, USER_CRN));

        assertThat(exception.getMessage()).contains("in progress");
    }

    @Test
    void cancelSkipNextWindowDeletesStoredSkipAfterScheduleChange() {
        MaintenanceWindowSchedule schedule = schedule();
        schedule.setDayOfWeek(DayOfWeek.TUESDAY);
        schedule.setStartLocalTime("10:00");

        MaintenanceWindowSkip storedSkip = skip(schedule, NEXT_WINDOW_START, Instant.parse("2025-01-13T10:00:00Z").toEpochMilli(), 1L);
        when(scheduleService.findRequired(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(schedule);
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(skipRepository.findByMaintenanceWindowScheduleId(schedule.getId())).thenReturn(List.of(storedSkip));

        MaintenanceWindowSkipResponse response = skipService.cancelSkipNextWindow(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID);

        assertThat(response.getWindowStart()).isEqualTo(NEXT_WINDOW_START);
        verify(skipRepository).delete(storedSkip);
    }

    @Test
    void cancelSkipNextWindowPrefersSkipCoveringNow() {
        MaintenanceWindowSchedule schedule = schedule();
        long duringWindow = Instant.parse("2025-01-06T09:30:00Z").toEpochMilli();
        long inProgressStart = Instant.parse("2025-01-06T09:00:00Z").toEpochMilli();
        long inProgressEnd = Instant.parse("2025-01-06T10:00:00Z").toEpochMilli();
        MaintenanceWindowSkip inProgressSkip = skip(schedule, inProgressStart, inProgressEnd, 1L);
        MaintenanceWindowSkip futureSkip = skip(schedule, NEXT_WINDOW_START, Instant.parse("2025-01-13T10:00:00Z").toEpochMilli(), 2L);

        when(scheduleService.findRequired(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(schedule);
        when(clock.getCurrentTimeMillis()).thenReturn(duringWindow);
        when(skipRepository.findByMaintenanceWindowScheduleId(schedule.getId()))
                .thenReturn(List.of(futureSkip, inProgressSkip));

        skipService.cancelSkipNextWindow(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID);

        verify(skipRepository).delete(inProgressSkip);
    }

    @Test
    void cancelSkipNextWindowReturnsNotFoundWhenNoUpcomingSkipExists() {
        MaintenanceWindowSchedule schedule = schedule();
        when(scheduleService.findRequired(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(schedule);
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(skipRepository.findByMaintenanceWindowScheduleId(schedule.getId())).thenReturn(List.of());

        assertThrows(NotFoundException.class,
                () -> skipService.cancelSkipNextWindow(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID));
    }

    private MaintenanceWindowSkip skip(MaintenanceWindowSchedule schedule, long windowStart, long windowEnd, long id) {
        MaintenanceWindowSkip skip = new MaintenanceWindowSkip();
        skip.setId(id);
        skip.setMaintenanceWindowSchedule(schedule);
        skip.setWindowStart(windowStart);
        skip.setWindowEnd(windowEnd);
        skip.setTimezone("UTC");
        skip.setCreatedAt(NOW);
        skip.setCreatedBy(USER_CRN);
        return skip;
    }

    private MaintenanceWindowSchedule schedule() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setId(1L);
        schedule.setAccountId(ACCOUNT_ID);
        schedule.setName("tenant-default");
        schedule.setScopeType(MaintenanceScopeType.TENANT);
        schedule.setScopeId(ACCOUNT_ID);
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        return schedule;
    }
}
