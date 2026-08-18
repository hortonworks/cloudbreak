package com.sequenceiq.maintenance.service;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleFieldsRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowOccurrenceResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSkipResponse;
import com.sequenceiq.maintenance.domain.MaintenanceEnumValues;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;

@Component
public class MaintenanceWindowScheduleConverter {

    private static final int UPCOMING_OCCURRENCE_LIMIT = 5;

    private static final int TEEN_ORDINAL_START = 11;

    private static final int TEEN_ORDINAL_END = 13;

    private static final int ORDINAL_ONES_PLACE = 10;

    private static final int ORDINAL_SUFFIX_FIRST = 1;

    private static final int ORDINAL_SUFFIX_SECOND = 2;

    private static final int ORDINAL_SUFFIX_THIRD = 3;

    private final MaintenanceOccurrenceCalculator occurrenceCalculator;

    private final Clock clock;

    @Inject
    public MaintenanceWindowScheduleConverter(MaintenanceOccurrenceCalculator occurrenceCalculator, Clock clock) {
        this.occurrenceCalculator = occurrenceCalculator;
        this.clock = clock;
    }

    public MaintenanceWindowSchedule toEntity(MaintenanceWindowScheduleRequest request) {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        applyRequest(schedule, request);
        return schedule;
    }

    public void applyRequest(MaintenanceWindowSchedule schedule, MaintenanceWindowScheduleRequest request) {
        schedule.setScopeType(MaintenanceEnumValues.toScopeType(request.getScopeType()));
        schedule.setScopeId(request.getScopeId());
        applyCreateMutableFields(schedule, request);
    }

    public void applyUpdateRequest(MaintenanceWindowSchedule schedule, UpdateMaintenanceWindowScheduleRequest request) {
        applyPatchMutableFields(schedule, request);
    }

    private void applyCreateMutableFields(MaintenanceWindowSchedule schedule, MaintenanceWindowScheduleFieldsRequest request) {
        schedule.setName(request.getName());
        schedule.setRecurrenceKind(MaintenanceEnumValues.toRecurrenceKind(request.getRecurrenceKind()));
        if (StringUtils.isNotBlank(request.getTimezone())) {
            schedule.setTimezone(request.getTimezone());
        }
        schedule.setDescription(request.getDescription());
        schedule.setDurationMinutes(request.getDurationMinutes());
        schedule.setStartLocalTime(request.getStartLocalTime());
        schedule.setDayOfWeek(MaintenanceEnumValues.toDayOfWeek(request.getDayOfWeek()));
        schedule.setWeekOrdinal(request.getWeekOrdinal());
        schedule.setDayOfMonth(request.getDayOfMonth());
        schedule.setCronExpression(request.getCronExpression());
    }

    private void applyPatchMutableFields(MaintenanceWindowSchedule schedule, MaintenanceWindowScheduleFieldsRequest request) {
        patchIfPresent(request.getName(), schedule::setName);
        boolean recurrenceKindPatched = request.getRecurrenceKind() != null;
        patchIfPresent(request.getRecurrenceKind(), value -> schedule.setRecurrenceKind(MaintenanceEnumValues.toRecurrenceKind(value)));
        patchIfPresent(request.getTimezone(), schedule::setTimezone);
        patchIfPresent(request.getDescription(), schedule::setDescription);
        patchIfPresent(request.getDurationMinutes(), schedule::setDurationMinutes);
        patchIfPresent(request.getStartLocalTime(), schedule::setStartLocalTime);
        patchIfPresent(request.getDayOfWeek(), value -> schedule.setDayOfWeek(MaintenanceEnumValues.toDayOfWeek(value)));
        patchIfPresent(request.getWeekOrdinal(), schedule::setWeekOrdinal);
        patchIfPresent(request.getDayOfMonth(), schedule::setDayOfMonth);
        patchIfPresent(request.getCronExpression(), schedule::setCronExpression);
        if (recurrenceKindPatched) {
            clearIrrelevantRecurrenceFields(schedule);
        }
    }

    private static void clearIrrelevantRecurrenceFields(MaintenanceWindowSchedule schedule) {
        switch (schedule.getRecurrenceKind()) {
            case WEEKLY -> {
                schedule.setWeekOrdinal(null);
                schedule.setDayOfMonth(null);
                schedule.setCronExpression(null);
            }
            case MONTHLY_NTH_WEEKDAY -> {
                schedule.setDayOfMonth(null);
                schedule.setCronExpression(null);
            }
            case MONTHLY_DAY_OF_MONTH -> {
                schedule.setDayOfWeek(null);
                schedule.setWeekOrdinal(null);
                schedule.setCronExpression(null);
            }
            case CRON -> {
                schedule.setStartLocalTime(null);
                schedule.setDayOfWeek(null);
                schedule.setWeekOrdinal(null);
                schedule.setDayOfMonth(null);
            }
            default -> throw new IllegalStateException("Unsupported recurrenceKind: " + schedule.getRecurrenceKind());
        }
    }

    private static <T> void patchIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    public MaintenanceWindowScheduleResponse toResponse(MaintenanceWindowSchedule schedule) {
        MaintenanceWindowScheduleResponse response = new MaintenanceWindowScheduleResponse();
        response.setId(schedule.getId());
        response.setAccountId(schedule.getAccountId());
        response.setName(schedule.getName());
        response.setScopeType(schedule.getScopeType().name());
        response.setScopeId(schedule.getScopeId());
        response.setRecurrenceKind(schedule.getRecurrenceKind().name());
        response.setTimezone(schedule.getTimezone());
        response.setDescription(schedule.getDescription());
        response.setDurationMinutes(schedule.getDurationMinutes());
        response.setStartLocalTime(schedule.getStartLocalTime());
        response.setDayOfWeek(schedule.getDayOfWeek() != null ? schedule.getDayOfWeek().name() : null);
        response.setWeekOrdinal(schedule.getWeekOrdinal());
        response.setDayOfMonth(schedule.getDayOfMonth());
        response.setCronExpression(schedule.getCronExpression());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());
        response.setCreatedBy(schedule.getCreatedBy());
        response.setUpdatedBy(schedule.getUpdatedBy());
        response.setVersion(schedule.getVersion());
        response.setRecurrenceSummary(buildRecurrenceSummary(schedule));

        long now = clock.getCurrentTimeMillis();
        List<WindowOccurrence> upcomingOccurrences = occurrenceCalculator.listUpcomingOccurrences(schedule, now);
        upcomingOccurrences.stream().findFirst().ifPresent(next -> {
            response.setNextOccurrenceStart(next.windowStart());
            response.setNextOccurrenceEnd(next.windowEnd());
        });
        response.setUpcomingOccurrences(upcomingOccurrences.stream()
                .limit(UPCOMING_OCCURRENCE_LIMIT)
                .map(occurrence -> new MaintenanceWindowOccurrenceResponse(occurrence.windowStart(), occurrence.windowEnd()))
                .toList());
        return response;
    }

    public MaintenanceWindowSkipResponse toSkipResponse(MaintenanceWindowSkip skip) {
        MaintenanceWindowSkipResponse response = new MaintenanceWindowSkipResponse();
        response.setId(skip.getId());
        response.setMaintenanceWindowScheduleId(skip.getMaintenanceWindowSchedule().getId());
        response.setWindowStart(skip.getWindowStart());
        response.setWindowEnd(skip.getWindowEnd());
        response.setTimezone(skip.getTimezone());
        response.setCreatedAt(skip.getCreatedAt());
        response.setCreatedBy(skip.getCreatedBy());
        response.setReason(skip.getReason());
        return response;
    }

    private String buildRecurrenceSummary(MaintenanceWindowSchedule schedule) {
        int durationMinutes = schedule.getDurationMinutes();
        return switch (schedule.getRecurrenceKind()) {
            case WEEKLY -> String.format("Every %s at %s, %d-minute windows (%s)",
                    formatDayOfWeek(schedule.getDayOfWeek()), schedule.getStartLocalTime(), durationMinutes, schedule.getTimezone());
            case MONTHLY_NTH_WEEKDAY -> String.format("Monthly on the %d%s %s at %s, %d-minute windows (%s)",
                    schedule.getWeekOrdinal(), ordinalSuffix(schedule.getWeekOrdinal()), formatDayOfWeek(schedule.getDayOfWeek()),
                    schedule.getStartLocalTime(), durationMinutes, schedule.getTimezone());
            case MONTHLY_DAY_OF_MONTH -> String.format("Monthly on day %d at %s, %d-minute windows (%s)",
                    schedule.getDayOfMonth(), schedule.getStartLocalTime(), durationMinutes, schedule.getTimezone());
            case CRON -> String.format("Custom cron schedule, %d-minute windows (%s)",
                    durationMinutes, schedule.getTimezone());
        };
    }

    private String formatDayOfWeek(DayOfWeek dayOfWeek) {
        return dayOfWeek == null ? "" : dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ROOT);
    }

    private String ordinalSuffix(int value) {
        if (value >= TEEN_ORDINAL_START && value <= TEEN_ORDINAL_END) {
            return "th";
        }
        return switch (value % ORDINAL_ONES_PLACE) {
            case ORDINAL_SUFFIX_FIRST -> "st";
            case ORDINAL_SUFFIX_SECOND -> "nd";
            case ORDINAL_SUFFIX_THIRD -> "rd";
            default -> "th";
        };
    }
}
