package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MaintenanceWindowScheduleRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsValidWeeklySchedule() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsValidCronSchedule() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setRecurrenceKind("CRON");
        request.setDayOfWeek(null);
        request.setStartLocalTime(null);
        request.setCronExpression("0 0 9 ? * MON *");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingRecurrenceKind() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setRecurrenceKind(null);

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("recurrenceKind"));
    }

    @Test
    void rejectsDurationBelowMinimum() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setDurationMinutes(30);

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "durationMinutes".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidRecurrenceKind() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setRecurrenceKind("DAILY");

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("recurrenceKind"));
    }

    @Test
    void rejectsWeeklyWithoutDayOfWeek() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setDayOfWeek(null);

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsWeeklyWithoutStartLocalTime() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setStartLocalTime(null);

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsCronWithoutExpression() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setRecurrenceKind("CRON");
        request.setDayOfWeek(null);
        request.setStartLocalTime(null);
        request.setCronExpression(null);

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidTimezoneWhenProvided() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setTimezone("Not/A/Timezone");

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void acceptsOmittedTimezone() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setTimezone(null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsWeeklyWithCronExpression() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setCronExpression("0 0 9 ? * MON *");

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsCronWithStructuredFields() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setRecurrenceKind("CRON");
        request.setCronExpression("0 0 9 ? * MON *");

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidStartLocalTimeFormat() {
        MaintenanceWindowScheduleRequest request = weeklyRequest();
        request.setStartLocalTime("9:00");

        Set<ConstraintViolation<MaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "startLocalTime".equals(v.getPropertyPath().toString()));
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
