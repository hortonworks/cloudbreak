package com.sequenceiq.distrox.api.v1.distrox.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

class DistroXV1RequestMaintenanceWindowScheduleValidationTest {

    private final Validator validator;

    DistroXV1RequestMaintenanceWindowScheduleValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void acceptsCreateWithoutMaintenanceWindowSchedule() {
        DistroXV1Request request = new DistroXV1Request();
        request.setMaintenanceWindowSchedule(null);

        assertThat(validator.validate(request))
                .noneMatch(v -> v.getPropertyPath().toString().startsWith("maintenanceWindowSchedule"));
    }

    @Test
    void acceptsValidNestedScheduleOnCreate() {
        DistroXV1Request request = new DistroXV1Request();
        request.setMaintenanceWindowSchedule(weeklySchedule());

        assertThat(validator.validate(request))
                .noneMatch(v -> v.getPropertyPath().toString().startsWith("maintenanceWindowSchedule"));
    }

    @Test
    void rejectsInvalidNestedScheduleOnCreate() {
        DistroXV1Request request = new DistroXV1Request();
        DistroXMaintenanceWindowScheduleRequest schedule = weeklySchedule();
        schedule.setRecurrenceKind(null);
        request.setMaintenanceWindowSchedule(schedule);

        Set<ConstraintViolation<DistroXV1Request>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "maintenanceWindowSchedule.recurrenceKind".equals(v.getPropertyPath().toString()));
    }

    private static DistroXMaintenanceWindowScheduleRequest weeklySchedule() {
        DistroXMaintenanceWindowScheduleRequest request = new DistroXMaintenanceWindowScheduleRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setDurationMinutes(60);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        return request;
    }
}
