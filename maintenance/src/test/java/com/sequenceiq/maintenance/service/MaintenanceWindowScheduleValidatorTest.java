package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

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

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleValidatorTest {

    private static final String ACCOUNT_ID = "acc-1";

    // 2025-01-01T00:00:00Z
    private static final long NOW = 1_735_689_600_000L;

    @Mock
    private Clock clock;

    private MaintenanceOccurrenceCalculator occurrenceCalculator;

    private MaintenanceWindowScheduleValidator validator;

    @BeforeEach
    void setUp() {
        occurrenceCalculator = new MaintenanceOccurrenceCalculator();
        validator = new MaintenanceWindowScheduleValidator(occurrenceCalculator, clock);
        lenient().when(clock.getCurrentTimeMillis()).thenReturn(NOW);
    }

    @Test
    void validateAcceptsWeeklySchedule() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        validator.validate(schedule);
    }

    @Test
    void validateRejectsDurationBelowMinimum() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setDurationMinutes(30);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule));
        assertThat(exception.getMessage()).contains("durationMinutes");
    }

    @Test
    void validateRejectsInvalidTimezone() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setTimezone("Not/A/Timezone");
        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule));
        assertThat(exception.getMessage()).contains("timezone");
    }

    @Test
    void validateRejectsTenantScopeWithMismatchedScopeId() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setScopeId("other-account");
        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule));
        assertThat(exception.getMessage()).contains("scopeId must equal accountId");
    }

    @Test
    void validateRejectsOverlyFrequentCronBeforeWalkingFullHorizon() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.CRON);
        schedule.setCronExpression("* * * * * ?");
        schedule.setStartLocalTime(null);
        schedule.setDayOfWeek(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule));
        assertThat(exception.getMessage()).contains("too many occurrences");
    }

    @Test
    void validateRejectsCronWithoutOccurrenceWithinNinetyDays() {
        MaintenanceWindowSchedule schedule = weeklySchedule("default");
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.CRON);
        schedule.setCronExpression("0 0 0 1 1 ? 2099");
        schedule.setStartLocalTime(null);
        schedule.setDayOfWeek(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validate(schedule));
        assertThat(exception.getMessage()).contains("90 days");
    }

    @Test
    void validatePreviewUnscopedRejectsDraftViolatingTwentyFourHourGap() {
        MaintenanceWindowSchedule draft = weeklySchedule("draft");
        draft.setRecurrenceKind(MaintenanceRecurrenceKind.CRON);
        draft.setCronExpression("0 0 9,21 ? * MON *");
        draft.setStartLocalTime(null);
        draft.setDayOfWeek(null);
        draft.setDurationMinutes(60);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validatePreview(draft, false));
        assertThat(exception.getMessage()).contains("24 hours");
    }

    @Test
    void validatePreviewScopedRunsScopeValidation() {
        MaintenanceWindowSchedule draft = weeklySchedule("draft");
        draft.setScopeId("other-account");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> validator.validatePreview(draft, true));
        assertThat(exception.getMessage()).contains("scopeId must equal accountId");
    }

    @Test
    void validatePreviewScopedValidatesDraftAloneLikePatch() {
        MaintenanceWindowSchedule draft = weeklySchedule("draft");
        draft.setScopeType(MaintenanceScopeType.TENANT);
        draft.setScopeId(ACCOUNT_ID);
        draft.setStartLocalTime("10:30");
        draft.setDurationMinutes(120);

        validator.validatePreview(draft, true);
    }

    private MaintenanceWindowSchedule weeklySchedule(String name) {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setAccountId(ACCOUNT_ID);
        schedule.setName(name);
        schedule.setScopeType(MaintenanceScopeType.TENANT);
        schedule.setScopeId(ACCOUNT_ID);
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(java.time.DayOfWeek.MONDAY);
        return schedule;
    }
}
