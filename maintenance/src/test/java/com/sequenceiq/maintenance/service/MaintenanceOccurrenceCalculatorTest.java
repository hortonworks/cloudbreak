package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;

class MaintenanceOccurrenceCalculatorTest {

    private final MaintenanceOccurrenceCalculator calculator = new MaintenanceOccurrenceCalculator();

    @Test
    void expandWeeklyOccurrences() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);

        long rangeStart = Instant.parse("2025-01-01T00:00:00Z").toEpochMilli();
        long rangeEnd = Instant.parse("2025-02-01T00:00:00Z").toEpochMilli();
        List<WindowOccurrence> occurrences = calculator.expandOccurrences(schedule, rangeStart, rangeEnd);

        assertThat(occurrences).hasSize(4);
        assertThat(occurrences.get(0).windowStart()).isEqualTo(Instant.parse("2025-01-06T09:00:00Z").toEpochMilli());
        assertThat(occurrences.get(0).windowEnd()).isEqualTo(Instant.parse("2025-01-06T10:00:00Z").toEpochMilli());
    }

    @Test
    void expandMonthlyNthWeekdayOccurrences() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.MONTHLY_NTH_WEEKDAY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setWeekOrdinal(2);

        long rangeStart = Instant.parse("2025-01-01T00:00:00Z").toEpochMilli();
        long rangeEnd = Instant.parse("2025-03-01T00:00:00Z").toEpochMilli();
        List<WindowOccurrence> occurrences = calculator.expandOccurrences(schedule, rangeStart, rangeEnd);

        assertThat(occurrences).hasSize(2);
        assertThat(occurrences.get(0).windowStart()).isEqualTo(Instant.parse("2025-01-13T09:00:00Z").toEpochMilli());
        assertThat(occurrences.get(1).windowStart()).isEqualTo(Instant.parse("2025-02-10T09:00:00Z").toEpochMilli());
    }

    @Test
    void expandMonthlyNthWeekdaySkipsMonthsWithoutEnoughWeekdays() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.MONTHLY_NTH_WEEKDAY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.FRIDAY);
        schedule.setWeekOrdinal(5);

        long rangeStart = Instant.parse("2025-04-01T00:00:00Z").toEpochMilli();
        long rangeEnd = Instant.parse("2025-06-01T00:00:00Z").toEpochMilli();
        List<WindowOccurrence> occurrences = calculator.expandOccurrences(schedule, rangeStart, rangeEnd);

        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.get(0).windowStart()).isEqualTo(Instant.parse("2025-05-30T09:00:00Z").toEpochMilli());
    }

    @Test
    void expandMonthlyDayOfMonthClampsShortMonths() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.MONTHLY_DAY_OF_MONTH);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("12:00");
        schedule.setDayOfMonth(31);

        long rangeStart = Instant.parse("2025-01-01T00:00:00Z").toEpochMilli();
        long rangeEnd = Instant.parse("2025-04-01T00:00:00Z").toEpochMilli();
        List<WindowOccurrence> occurrences = calculator.expandOccurrences(schedule, rangeStart, rangeEnd);

        assertThat(occurrences).hasSize(3);
        assertThat(occurrences.get(1).windowStart()).isEqualTo(Instant.parse("2025-02-28T12:00:00Z").toEpochMilli());
    }

    @Test
    void findNextUpcomingOccurrenceSkipsPastWindows() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setAccountId("acc");
        schedule.setName("weekly");
        schedule.setScopeType(MaintenanceScopeType.TENANT);
        schedule.setScopeId("acc");
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);

        long now = Instant.parse("2025-01-06T10:30:00Z").toEpochMilli();
        WindowOccurrence next = calculator.findNextUpcomingOccurrence(schedule, now).orElseThrow();

        assertThat(next.windowStart()).isEqualTo(Instant.parse("2025-01-13T09:00:00Z").toEpochMilli());
    }

    @Test
    void listUpcomingOccurrencesIncludesInProgressWindowAsFirstEntry() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);

        long now = Instant.parse("2025-01-06T09:30:00Z").toEpochMilli();
        List<WindowOccurrence> upcoming = calculator.listUpcomingOccurrences(schedule, now);

        assertThat(upcoming).isNotEmpty();
        assertThat(upcoming.get(0).windowStart()).isEqualTo(Instant.parse("2025-01-06T09:00:00Z").toEpochMilli());
        assertThat(calculator.findNextUpcomingOccurrence(schedule, now)).contains(upcoming.get(0));
    }
}
