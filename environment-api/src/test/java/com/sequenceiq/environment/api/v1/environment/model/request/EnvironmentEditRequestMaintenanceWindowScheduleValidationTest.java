package com.sequenceiq.environment.api.v1.environment.model.request;

import static com.sequenceiq.environment.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class EnvironmentEditRequestMaintenanceWindowScheduleValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsEditWithoutMaintenanceWindowScheduleFields() {
        assertThat(validator.validate(new EnvironmentEditRequest())).isEmpty();
    }

    @Test
    void acceptsClearWithoutSchedulePatch() {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        request.setClearMaintenanceWindowSchedule(true);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsValidNestedSchedulePatch() {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        request.setMaintenanceWindowSchedule(partialWeeklySchedule());

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsClearCombinedWithSchedulePatch() {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        request.setClearMaintenanceWindowSchedule(true);
        request.setMaintenanceWindowSchedule(partialWeeklySchedule());

        Set<ConstraintViolation<EnvironmentEditRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "maintenanceWindowScheduleEditConsistent".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidNestedSchedulePatch() {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        EnvironmentMaintenanceWindowScheduleEditRequest schedule = partialWeeklySchedule();
        schedule.setDurationMinutes(30);
        request.setMaintenanceWindowSchedule(schedule);

        Set<ConstraintViolation<EnvironmentEditRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "maintenanceWindowSchedule.durationMinutes".equals(v.getPropertyPath().toString()));
    }

    private static EnvironmentMaintenanceWindowScheduleEditRequest partialWeeklySchedule() {
        EnvironmentMaintenanceWindowScheduleEditRequest schedule = new EnvironmentMaintenanceWindowScheduleEditRequest();
        schedule.setRecurrenceKind("WEEKLY");
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek("MONDAY");
        schedule.setDurationMinutes(60);
        return schedule;
    }
}
