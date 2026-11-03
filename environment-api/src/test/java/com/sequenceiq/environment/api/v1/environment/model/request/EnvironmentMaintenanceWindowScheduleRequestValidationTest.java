package com.sequenceiq.environment.api.v1.environment.model.request;

import static com.sequenceiq.environment.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class EnvironmentMaintenanceWindowScheduleRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsValidWeeklySchedule() {
        assertThat(validator.validate(weeklySchedule())).isEmpty();
    }

    @Test
    void acceptsValidCronSchedule() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setRecurrenceKind("CRON");
        request.setDayOfWeek(null);
        request.setStartLocalTime(null);
        request.setCronExpression("0 0 9 ? * MON *");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingRecurrenceKind() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setRecurrenceKind(null);

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("recurrenceKind"));
    }

    @Test
    void rejectsMissingDurationMinutes() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setDurationMinutes(null);

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("durationMinutes"));
    }

    @Test
    void rejectsDurationBelowMinimum() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setDurationMinutes(30);

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "durationMinutes".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidRecurrenceKind() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setRecurrenceKind("DAILY");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().endsWith("recurrenceKind"));
    }

    @Test
    void rejectsWeeklyWithoutDayOfWeek() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setDayOfWeek(null);

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsWeeklyWithoutStartLocalTime() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setStartLocalTime(null);

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsCronWithoutExpression() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setRecurrenceKind("CRON");
        request.setDayOfWeek(null);
        request.setStartLocalTime(null);
        request.setCronExpression(null);

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidTimezoneWhenProvided() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setTimezone("Not/A/Timezone");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void acceptsOmittedTimezone() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setTimezone(null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsWeeklyWithCronExpression() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setCronExpression("0 0 9 ? * MON *");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidStartLocalTimeFormat() {
        EnvironmentMaintenanceWindowScheduleRequest request = weeklySchedule();
        request.setStartLocalTime("9:00");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "startLocalTime".equals(v.getPropertyPath().toString()));
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
