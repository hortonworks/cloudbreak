package com.sequenceiq.maintenance.api.v1.schedule.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;

class MaintenanceScheduleRecurrenceFieldValidatorTest {

    @Test
    void validatesMonthlyNthWeekdayRequiresWeekOrdinal() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setRecurrenceKind("MONTHLY_NTH_WEEKDAY");
        request.setWeekOrdinal(null);

        assertThat(MaintenanceScheduleRecurrenceFieldValidator.isValidForCreate(request)).isFalse();
    }

    @Test
    void validatesMonthlyDayOfMonthRequiresDayOfMonth() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setRecurrenceKind("MONTHLY_DAY_OF_MONTH");
        request.setDayOfWeek(null);
        request.setDayOfMonth(null);

        assertThat(MaintenanceScheduleRecurrenceFieldValidator.isValidForCreate(request)).isFalse();
    }

    @Test
    void rejectsWeeklyWithDayOfMonth() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setDayOfMonth(15);

        assertThat(MaintenanceScheduleRecurrenceFieldValidator.isValidForCreate(request)).isFalse();
    }

    @Test
    void patchSkipsRecurrenceValidationWhenKindOmitted() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setName("updated");

        assertThat(MaintenanceScheduleRecurrenceFieldValidator.isValidForPatch(request)).isTrue();
    }

    private static MaintenanceWindowScheduleRequest weeklyRequest() {
        MaintenanceWindowScheduleRequest request = new MaintenanceWindowScheduleRequest();
        request.setScopeType("ENVIRONMENT");
        request.setScopeId("crn:cdp:environments:us-west-1:123:environment:abc");
        request.setRecurrenceKind("WEEKLY");
        request.setDurationMinutes(60);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        return request;
    }
}
