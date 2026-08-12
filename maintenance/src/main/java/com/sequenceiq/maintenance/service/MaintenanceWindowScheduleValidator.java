package com.sequenceiq.maintenance.service;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

@Component
public class MaintenanceWindowScheduleValidator {

    public static final long OCCURRENCE_HORIZON_MS = 90L * 24 * 60 * 60 * 1000;

    public static final int MIN_DURATION_MINUTES = 60;

    private static final long MIN_WINDOW_START_GAP_MS = 24L * 60 * 60 * 1000;

    private static final int MIN_WEEK_ORDINAL = 1;

    private static final int MAX_WEEK_ORDINAL = 5;

    private static final int MIN_DAY_OF_MONTH = 1;

    private static final int MAX_DAY_OF_MONTH = 31;

    private final MaintenanceWindowScheduleRepository scheduleRepository;

    private final MaintenanceOccurrenceCalculator occurrenceCalculator;

    private final Clock clock;

    @Inject
    public MaintenanceWindowScheduleValidator(
            MaintenanceWindowScheduleRepository scheduleRepository,
            MaintenanceOccurrenceCalculator occurrenceCalculator,
            Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.occurrenceCalculator = occurrenceCalculator;
        this.clock = clock;
    }

    public void validate(MaintenanceWindowSchedule schedule, Long excludeScheduleId) {
        validateRequiredFields(schedule);
        validateDuration(schedule.getDurationMinutes());
        validateTimezone(schedule.getTimezone());
        validateRecurrenceFields(schedule);
        validateScope(schedule);
        validateOccurrences(schedule, excludeScheduleId);
    }

    private void validateRequiredFields(MaintenanceWindowSchedule schedule) {
        if (StringUtils.isBlank(schedule.getAccountId())) {
            throw new BadRequestException("accountId must not be blank.");
        }
        if (schedule.getScopeType() == null) {
            throw new BadRequestException("scopeType must not be blank.");
        }
        if (StringUtils.isBlank(schedule.getScopeId())) {
            throw new BadRequestException("scopeId must not be blank.");
        }
        if (schedule.getRecurrenceKind() == null) {
            throw new BadRequestException("recurrenceKind must not be blank.");
        }
    }

    private void validateDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < MIN_DURATION_MINUTES) {
            throw new BadRequestException("durationMinutes must be at least " + MIN_DURATION_MINUTES + ".");
        }
    }

    private void validateTimezone(String timezone) {
        if (StringUtils.isBlank(timezone)) {
            throw new BadRequestException("timezone must not be blank.");
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new BadRequestException("timezone must be a valid IANA timezone: " + timezone);
        }
    }

    private void validateRecurrenceFields(MaintenanceWindowSchedule schedule) {
        switch (schedule.getRecurrenceKind()) {
            case WEEKLY -> validateWeekly(schedule);
            case MONTHLY_NTH_WEEKDAY -> validateMonthlyNthWeekday(schedule);
            case MONTHLY_DAY_OF_MONTH -> validateMonthlyDayOfMonth(schedule);
            case CRON -> validateCron(schedule);
            default -> throw new BadRequestException("Unsupported recurrenceKind: " + schedule.getRecurrenceKind());
        }
    }

    private void validateWeekly(MaintenanceWindowSchedule schedule) {
        requireStartLocalTime(schedule);
        if (schedule.getDayOfWeek() == null) {
            throw new BadRequestException("dayOfWeek is required for WEEKLY recurrence.");
        }
    }

    private void validateMonthlyNthWeekday(MaintenanceWindowSchedule schedule) {
        requireStartLocalTime(schedule);
        if (schedule.getDayOfWeek() == null) {
            throw new BadRequestException("dayOfWeek is required for MONTHLY_NTH_WEEKDAY recurrence.");
        }
        if (schedule.getWeekOrdinal() == null || schedule.getWeekOrdinal() < MIN_WEEK_ORDINAL || schedule.getWeekOrdinal() > MAX_WEEK_ORDINAL) {
            throw new BadRequestException("weekOrdinal must be between " + MIN_WEEK_ORDINAL + " and " + MAX_WEEK_ORDINAL
                    + " for MONTHLY_NTH_WEEKDAY recurrence.");
        }
    }

    private void validateMonthlyDayOfMonth(MaintenanceWindowSchedule schedule) {
        requireStartLocalTime(schedule);
        if (schedule.getDayOfMonth() == null || schedule.getDayOfMonth() < MIN_DAY_OF_MONTH || schedule.getDayOfMonth() > MAX_DAY_OF_MONTH) {
            throw new BadRequestException("dayOfMonth must be between " + MIN_DAY_OF_MONTH + " and " + MAX_DAY_OF_MONTH
                    + " for MONTHLY_DAY_OF_MONTH recurrence.");
        }
    }

    private void validateCron(MaintenanceWindowSchedule schedule) {
        if (StringUtils.isBlank(schedule.getCronExpression())) {
            throw new BadRequestException("cronExpression is required for CRON recurrence.");
        }
        if (!CronExpression.isValidExpression(schedule.getCronExpression())) {
            throw new BadRequestException("cronExpression is not a valid Quartz cron expression.");
        }
    }

    private void requireStartLocalTime(MaintenanceWindowSchedule schedule) {
        if (StringUtils.isBlank(schedule.getStartLocalTime())) {
            throw new BadRequestException("startLocalTime is required for " + schedule.getRecurrenceKind() + " recurrence.");
        }
        try {
            LocalTime.parse(schedule.getStartLocalTime());
        } catch (DateTimeException e) {
            throw new BadRequestException("startLocalTime must be in HH:mm format.");
        }
    }

    private void validateScope(MaintenanceWindowSchedule schedule) {
        if (schedule.getScopeType() == MaintenanceScopeType.TENANT
                && !Objects.equals(schedule.getAccountId(), schedule.getScopeId())) {
            throw new BadRequestException("scopeId must equal accountId for TENANT scope.");
        }
    }

    private void validateOccurrences(MaintenanceWindowSchedule schedule, Long excludeScheduleId) {
        long now = clock.getCurrentTimeMillis();
        long horizonEnd = now + OCCURRENCE_HORIZON_MS;
        List<WindowOccurrence> candidateOccurrences = occurrenceCalculator.expandOccurrences(schedule, now, horizonEnd);
        if (candidateOccurrences.isEmpty()) {
            throw new BadRequestException("Schedule must have at least one occurrence within the next 90 days.");
        }

        List<WindowOccurrence> allOccurrences = new ArrayList<>(candidateOccurrences);
        scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                        schedule.getAccountId(), schedule.getScopeType(), schedule.getScopeId())
                .filter(existing -> !Objects.equals(existing.getId(), excludeScheduleId))
                .ifPresent(existing -> allOccurrences.addAll(
                        occurrenceCalculator.expandOccurrences(existing, now, horizonEnd)));

        allOccurrences.sort(Comparator.comparingLong(WindowOccurrence::windowStart));
        for (int i = 1; i < allOccurrences.size(); i++) {
            WindowOccurrence previous = allOccurrences.get(i - 1);
            WindowOccurrence current = allOccurrences.get(i);
            if (previous.overlaps(current)) {
                throw new BadRequestException("Schedule overlaps with another schedule at the same scope.");
            }
            if (current.windowStart() - previous.windowStart() < MIN_WINDOW_START_GAP_MS) {
                throw new BadRequestException("At most one maintenance window may start per 24 hours at the same scope.");
            }
        }
    }
}
