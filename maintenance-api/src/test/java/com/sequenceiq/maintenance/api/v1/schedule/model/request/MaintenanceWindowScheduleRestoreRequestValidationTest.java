package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MaintenanceWindowScheduleRestoreRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsValidRestoreRequest() {
        MaintenanceWindowScheduleRestoreRequest request = restoreRequest();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingId() {
        MaintenanceWindowScheduleRestoreRequest request = restoreRequest();
        request.setId(null);

        Set<ConstraintViolation<MaintenanceWindowScheduleRestoreRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "id".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsMissingRecurrenceKind() {
        MaintenanceWindowScheduleRestoreRequest request = restoreRequest();
        request.setRecurrenceKind(null);

        Set<ConstraintViolation<MaintenanceWindowScheduleRestoreRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("recurrenceKind"));
    }

    private static MaintenanceWindowScheduleRestoreRequest restoreRequest() {
        MaintenanceWindowScheduleRestoreRequest request = new MaintenanceWindowScheduleRestoreRequest();
        request.setId(42L);
        request.setScopeType("ENVIRONMENT");
        request.setScopeId("crn:cdp:environments:us-west-1:123:environment:abc");
        request.setRecurrenceKind("WEEKLY");
        request.setDurationMinutes(60);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        return request;
    }
}
