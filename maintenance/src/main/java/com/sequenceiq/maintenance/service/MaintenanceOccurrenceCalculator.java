package com.sequenceiq.maintenance.service;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.quartz.CronExpression;
import org.springframework.stereotype.Component;

import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;

@Component
public class MaintenanceOccurrenceCalculator {

    private static final long MILLIS_PER_MINUTE = 60_000L;

    public List<WindowOccurrence> expandOccurrences(MaintenanceWindowSchedule schedule, long rangeStartInclusiveMs, long rangeEndExclusiveMs) {
        ZoneId zone = ZoneId.of(schedule.getTimezone());
        long durationMs = schedule.getDurationMinutes() * MILLIS_PER_MINUTE;
        return switch (schedule.getRecurrenceKind()) {
            case WEEKLY -> expandWeekly(schedule, zone, durationMs, rangeStartInclusiveMs, rangeEndExclusiveMs);
            case MONTHLY_NTH_WEEKDAY -> expandMonthlyNthWeekday(schedule, zone, durationMs, rangeStartInclusiveMs, rangeEndExclusiveMs);
            case MONTHLY_DAY_OF_MONTH -> expandMonthlyDayOfMonth(schedule, zone, durationMs, rangeStartInclusiveMs, rangeEndExclusiveMs);
            case CRON -> expandCron(schedule, zone, durationMs, rangeStartInclusiveMs, rangeEndExclusiveMs);
        };
    }

    public Optional<WindowOccurrence> findNextUpcomingOccurrence(MaintenanceWindowSchedule schedule, long nowMs) {
        return listUpcomingOccurrences(schedule, nowMs).stream().findFirst();
    }

    /**
     * Occurrences whose window has not yet ended ({@code windowEnd > nowMs}), including any window currently in progress.
     */
    public List<WindowOccurrence> listUpcomingOccurrences(MaintenanceWindowSchedule schedule, long nowMs) {
        long durationMs = schedule.getDurationMinutes() * MILLIS_PER_MINUTE;
        long horizonEnd = nowMs + MaintenanceWindowScheduleValidator.OCCURRENCE_HORIZON_MS;
        return expandOccurrences(schedule, nowMs - durationMs, horizonEnd).stream()
                .filter(occurrence -> occurrence.windowEnd() > nowMs)
                .toList();
    }

    private List<WindowOccurrence> expandWeekly(
            MaintenanceWindowSchedule schedule, ZoneId zone, long durationMs, long rangeStartInclusiveMs, long rangeEndExclusiveMs) {
        List<WindowOccurrence> occurrences = new ArrayList<>();
        LocalTime startTime = LocalTime.parse(schedule.getStartLocalTime());
        DayOfWeek targetDay = DayOfWeek.valueOf(schedule.getDayOfWeek().name());
        ZonedDateTime cursor = Instant.ofEpochMilli(rangeStartInclusiveMs).atZone(zone);
        ZonedDateTime next = cursor.with(TemporalAdjusters.nextOrSame(targetDay)).with(startTime);
        if (next.toInstant().toEpochMilli() < rangeStartInclusiveMs) {
            next = next.plusWeeks(1);
        }
        while (next.toInstant().toEpochMilli() < rangeEndExclusiveMs) {
            addOccurrence(occurrences, next, durationMs);
            next = next.plusWeeks(1);
        }
        return occurrences;
    }

    private List<WindowOccurrence> expandMonthlyNthWeekday(
            MaintenanceWindowSchedule schedule, ZoneId zone, long durationMs, long rangeStartInclusiveMs, long rangeEndExclusiveMs) {
        List<WindowOccurrence> occurrences = new ArrayList<>();
        LocalTime startTime = LocalTime.parse(schedule.getStartLocalTime());
        DayOfWeek targetDay = DayOfWeek.valueOf(schedule.getDayOfWeek().name());
        ZonedDateTime monthCursor = Instant.ofEpochMilli(rangeStartInclusiveMs).atZone(zone).withDayOfMonth(1).with(startTime);
        ZonedDateTime rangeEnd = Instant.ofEpochMilli(rangeEndExclusiveMs).atZone(zone);
        while (!monthCursor.isAfter(rangeEnd)) {
            ZonedDateTime candidate = monthCursor.with(TemporalAdjusters.dayOfWeekInMonth(schedule.getWeekOrdinal(), targetDay))
                    .with(startTime);
            if (candidate.getMonth() == monthCursor.getMonth()) {
                long startMs = candidate.toInstant().toEpochMilli();
                if (startMs >= rangeStartInclusiveMs && startMs < rangeEndExclusiveMs) {
                    addOccurrence(occurrences, candidate, durationMs);
                }
            }
            monthCursor = monthCursor.plusMonths(1);
        }
        return occurrences;
    }

    private List<WindowOccurrence> expandMonthlyDayOfMonth(
            MaintenanceWindowSchedule schedule, ZoneId zone, long durationMs, long rangeStartInclusiveMs, long rangeEndExclusiveMs) {
        List<WindowOccurrence> occurrences = new ArrayList<>();
        LocalTime startTime = LocalTime.parse(schedule.getStartLocalTime());
        ZonedDateTime monthCursor = Instant.ofEpochMilli(rangeStartInclusiveMs).atZone(zone).withDayOfMonth(1).with(startTime);
        ZonedDateTime rangeEnd = Instant.ofEpochMilli(rangeEndExclusiveMs).atZone(zone);
        while (!monthCursor.isAfter(rangeEnd)) {
            int lengthOfMonth = monthCursor.toLocalDate().lengthOfMonth();
            int dayOfMonth = Math.min(schedule.getDayOfMonth(), lengthOfMonth);
            ZonedDateTime candidate = monthCursor.withDayOfMonth(dayOfMonth).with(startTime);
            long startMs = candidate.toInstant().toEpochMilli();
            if (startMs >= rangeStartInclusiveMs && startMs < rangeEndExclusiveMs) {
                addOccurrence(occurrences, candidate, durationMs);
            }
            monthCursor = monthCursor.plusMonths(1);
        }
        return occurrences;
    }

    private List<WindowOccurrence> expandCron(
            MaintenanceWindowSchedule schedule, ZoneId zone, long durationMs, long rangeStartInclusiveMs, long rangeEndExclusiveMs) {
        List<WindowOccurrence> occurrences = new ArrayList<>();
        try {
            CronExpression cronExpression = new CronExpression(schedule.getCronExpression());
            cronExpression.setTimeZone(TimeZone.getTimeZone(zone));
            Date after = new Date(rangeStartInclusiveMs - 1);
            while (true) {
                Date next = cronExpression.getNextValidTimeAfter(after);
                if (next == null || next.getTime() >= rangeEndExclusiveMs) {
                    break;
                }
                if (next.getTime() >= rangeStartInclusiveMs) {
                    occurrences.add(new WindowOccurrence(next.getTime(), next.getTime() + durationMs));
                }
                after = next;
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + schedule.getCronExpression(), e);
        }
        return occurrences;
    }

    private void addOccurrence(List<WindowOccurrence> occurrences, ZonedDateTime start, long durationMs) {
        long startMs = start.toInstant().toEpochMilli();
        occurrences.add(new WindowOccurrence(startMs, startMs + durationMs));
    }
}
