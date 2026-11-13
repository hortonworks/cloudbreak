package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSchedulePreviewRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSchedulePreviewResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowSkipRepository;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowSchedulePreviewServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final long NOW = Instant.parse("2025-01-01T12:00:00Z").toEpochMilli();

    @Mock
    private MaintenanceWindowScheduleRepository scheduleRepository;

    @Mock
    private MaintenanceWindowSkipRepository skipRepository;

    @Mock
    private Clock clock;

    private MaintenanceWindowSchedulePreviewService underTest;

    @BeforeEach
    void setUp() {
        MaintenanceOccurrenceCalculator occurrenceCalculator = new MaintenanceOccurrenceCalculator();
        MaintenanceWindowScheduleConverter converter = new MaintenanceWindowScheduleConverter(occurrenceCalculator, clock);
        MaintenanceWindowScheduleValidator validator = new MaintenanceWindowScheduleValidator(occurrenceCalculator, clock);
        underTest = new MaintenanceWindowSchedulePreviewService(
                converter,
                validator,
                occurrenceCalculator,
                scheduleRepository,
                skipRepository,
                clock);
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
    }

    @Test
    void previewWeeklyOccurrencesWithoutScope() {
        MaintenanceWindowSchedulePreviewRequest request = weeklyPreviewRequest();

        MaintenanceWindowSchedulePreviewResponse response = underTest.preview(request, ACCOUNT_ID);

        assertThat(response.getTimezone()).isEqualTo("UTC");
        assertThat(response.getOccurrences()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(response.getOccurrences().get(0).isSkipped()).isFalse();
    }

    @Test
    void previewMarksPersistedSkipsWhenScopeProvided() {
        MaintenanceWindowSchedulePreviewRequest request = weeklyPreviewRequest();
        request.setScopeType(MaintenanceScopeType.TENANT.name());
        request.setScopeId(ACCOUNT_ID);

        MaintenanceWindowSchedule persisted = persistedWeeklySchedule(10L);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(java.util.Optional.of(persisted));

        long skippedStart = Instant.parse("2025-01-06T09:00:00Z").toEpochMilli();
        MaintenanceWindowSkip skip = new MaintenanceWindowSkip();
        skip.setWindowStart(skippedStart);
        when(skipRepository.findByMaintenanceWindowScheduleId(10L)).thenReturn(java.util.List.of(skip));

        MaintenanceWindowSchedulePreviewResponse response = underTest.preview(request, ACCOUNT_ID);

        assertThat(response.getOccurrences())
                .anyMatch(occurrence -> occurrence.getWindowStart().equals(skippedStart) && occurrence.isSkipped());
    }

    @Test
    void previewWeeklySundayAfterDstSpringForwardUsesConfiguredLocalTime() {
        long previewFrom = Instant.parse("2025-03-01T00:00:00Z").toEpochMilli();
        when(clock.getCurrentTimeMillis()).thenReturn(previewFrom);

        MaintenanceWindowSchedulePreviewRequest request = new MaintenanceWindowSchedulePreviewRequest();
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setDurationMinutes(60);
        request.setTimezone("America/New_York");
        request.setStartLocalTime("02:30");
        request.setDayOfWeek("SUNDAY");
        request.setLimit(10);

        MaintenanceWindowSchedulePreviewResponse response = underTest.preview(request, ACCOUNT_ID);

        long marchSixteenStart = Instant.parse("2025-03-16T06:30:00Z").toEpochMilli();
        assertThat(response.getOccurrences())
                .anyMatch(occurrence -> occurrence.getWindowStart().equals(marchSixteenStart));
        assertThat(response.getOccurrences())
                .noneMatch(occurrence -> occurrence.getWindowStart().equals(Instant.parse("2025-03-16T07:30:00Z").toEpochMilli()));
    }

    @Test
    void previewMatchesPersistedSkipAfterDstSpringForwardWeek() {
        long previewFrom = Instant.parse("2025-03-01T00:00:00Z").toEpochMilli();
        when(clock.getCurrentTimeMillis()).thenReturn(previewFrom);

        MaintenanceWindowSchedulePreviewRequest request = new MaintenanceWindowSchedulePreviewRequest();
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setDurationMinutes(60);
        request.setTimezone("America/New_York");
        request.setStartLocalTime("02:30");
        request.setDayOfWeek("SUNDAY");
        request.setLimit(10);
        request.setScopeType(MaintenanceScopeType.TENANT.name());
        request.setScopeId(ACCOUNT_ID);
        request.setIncludePersistedSkips(true);

        MaintenanceWindowSchedule persisted = persistedWeeklySchedule(10L);
        persisted.setTimezone("America/New_York");
        persisted.setStartLocalTime("02:30");
        persisted.setDayOfWeek(java.time.DayOfWeek.SUNDAY);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(java.util.Optional.of(persisted));

        long dispatchWindowStart = Instant.parse("2025-03-16T06:30:00Z").toEpochMilli();
        MaintenanceWindowSkip skip = new MaintenanceWindowSkip();
        skip.setWindowStart(dispatchWindowStart);
        when(skipRepository.findByMaintenanceWindowScheduleId(10L)).thenReturn(java.util.List.of(skip));

        MaintenanceWindowSchedulePreviewResponse response = underTest.preview(request, ACCOUNT_ID);

        assertThat(response.getOccurrences())
                .anyMatch(occurrence -> occurrence.getWindowStart().equals(dispatchWindowStart) && occurrence.isSkipped());
    }

    @Test
    void previewCronOccurrences() {
        MaintenanceWindowSchedulePreviewRequest request = new MaintenanceWindowSchedulePreviewRequest();
        request.setRecurrenceKind(MaintenanceRecurrenceKind.CRON.name());
        request.setDurationMinutes(60);
        request.setTimezone("UTC");
        request.setCronExpression("0 0 9 ? * MON *");

        MaintenanceWindowSchedulePreviewResponse response = underTest.preview(request, ACCOUNT_ID);

        assertThat(response.getOccurrences()).isNotEmpty();
    }

    private static MaintenanceWindowSchedule persistedWeeklySchedule(long id) {
        MaintenanceWindowSchedule persisted = new MaintenanceWindowSchedule();
        persisted.setId(id);
        persisted.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        persisted.setTimezone("UTC");
        persisted.setDurationMinutes(60);
        persisted.setStartLocalTime("09:00");
        persisted.setDayOfWeek(java.time.DayOfWeek.MONDAY);
        return persisted;
    }

    private static MaintenanceWindowSchedulePreviewRequest weeklyPreviewRequest() {
        MaintenanceWindowSchedulePreviewRequest request = new MaintenanceWindowSchedulePreviewRequest();
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setDurationMinutes(60);
        request.setTimezone("UTC");
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        request.setLimit(3);
        return request;
    }
}
