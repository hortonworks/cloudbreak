package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleConverterTest {

    @Mock
    private Clock clock;

    private MaintenanceWindowScheduleConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MaintenanceWindowScheduleConverter(new MaintenanceOccurrenceCalculator(), clock);
    }

    @Test
    void updateClearsStaleFieldsWhenRecurrenceKindChanges() {
        MaintenanceWindowSchedule schedule = weeklySchedule();

        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setRecurrenceKind(MaintenanceRecurrenceKind.CRON.name());
        request.setCronExpression("0 0 9 ? * MON *");

        converter.applyUpdateRequest(schedule, request);

        assertThat(schedule.getRecurrenceKind()).isEqualTo(MaintenanceRecurrenceKind.CRON);
        assertThat(schedule.getCronExpression()).isEqualTo("0 0 9 ? * MON *");
        assertThat(schedule.getStartLocalTime()).isNull();
        assertThat(schedule.getDayOfWeek()).isNull();
        assertThat(schedule.getWeekOrdinal()).isNull();
        assertThat(schedule.getDayOfMonth()).isNull();
    }

    private static MaintenanceWindowSchedule weeklySchedule() {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setScopeType(MaintenanceScopeType.TENANT);
        schedule.setScopeId("acc-1");
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(120);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setWeekOrdinal(2);
        return schedule;
    }
}
