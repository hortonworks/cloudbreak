package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

class MaintenanceWindowScheduleOnCreateRequestValidationTest {

    private final Validator validator;

    MaintenanceWindowScheduleOnCreateRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void acceptsValidWeeklySchedule() {
        assertThat(validator.validate(weeklySchedule())).isEmpty();
    }

    @Test
    void rejectsMissingRecurrenceKind() {
        MaintenanceWindowScheduleOnCreateRequest request = weeklySchedule();
        request.setRecurrenceKind(null);

        Set<ConstraintViolation<MaintenanceWindowScheduleOnCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceKind".equals(v.getPropertyPath().toString()));
    }

    private static MaintenanceWindowScheduleOnCreateRequest weeklySchedule() {
        MaintenanceWindowScheduleOnCreateRequest request = new MaintenanceWindowScheduleOnCreateRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setDurationMinutes(60);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        return request;
    }
}
