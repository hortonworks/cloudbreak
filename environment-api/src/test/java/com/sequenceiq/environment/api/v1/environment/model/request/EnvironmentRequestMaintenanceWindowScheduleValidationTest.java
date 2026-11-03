package com.sequenceiq.environment.api.v1.environment.model.request;

import static com.sequenceiq.environment.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class EnvironmentRequestMaintenanceWindowScheduleValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsCreateWithoutMaintenanceWindowSchedule() {
        EnvironmentRequest request = minimalCreateRequest();
        request.setMaintenanceWindowSchedule(null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsValidNestedScheduleOnCreate() {
        EnvironmentRequest request = minimalCreateRequest();
        request.setMaintenanceWindowSchedule(weeklySchedule());

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidNestedScheduleOnCreate() {
        EnvironmentRequest request = minimalCreateRequest();
        EnvironmentMaintenanceWindowScheduleRequest schedule = weeklySchedule();
        schedule.setRecurrenceKind(null);
        request.setMaintenanceWindowSchedule(schedule);

        Set<ConstraintViolation<EnvironmentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "maintenanceWindowSchedule.recurrenceKind".equals(v.getPropertyPath().toString()));
    }

    private static EnvironmentRequest minimalCreateRequest() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setName("valid-env");
        LocationRequest location = new LocationRequest();
        location.setName("us-west-1");
        request.setLocation(location);
        return request;
    }

    private static EnvironmentMaintenanceWindowScheduleRequest weeklySchedule() {
        EnvironmentMaintenanceWindowScheduleRequest request = new EnvironmentMaintenanceWindowScheduleRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setDurationMinutes(60);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        return request;
    }
}
