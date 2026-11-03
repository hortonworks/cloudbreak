package com.sequenceiq.environment.api.v1.environment.model.request;

import static com.sequenceiq.environment.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class EnvironmentMaintenanceWindowScheduleEditRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsEmptyPatchBody() {
        assertThat(validator.validate(new EnvironmentMaintenanceWindowScheduleEditRequest())).isEmpty();
    }

    @Test
    void acceptsPartialNameUpdate() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setName("nightly window");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsRecurrenceKindChangeWithMatchingFields() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsRecurrenceKindChangeWithoutRequiredFields() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setRecurrenceKind("CRON");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleEditRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidCronExpressionWhenProvidedWithoutRecurrenceKind() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setCronExpression("not a cron");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleEditRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void acceptsValidCronExpressionWhenProvidedWithoutRecurrenceKind() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setCronExpression("0 0 9 ? * MON *");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsRecurrenceKindChangeWithIncompatibleFields() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        request.setCronExpression("0 0 9 ? * MON *");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleEditRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidTimezoneWhenProvided() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setTimezone("Not/A/Timezone");

        Set<ConstraintViolation<EnvironmentMaintenanceWindowScheduleEditRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }
}
