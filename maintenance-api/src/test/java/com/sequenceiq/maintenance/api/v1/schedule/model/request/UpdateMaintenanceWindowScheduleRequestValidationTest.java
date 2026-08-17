package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class UpdateMaintenanceWindowScheduleRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsEmptyPatchBody() {
        assertThat(validator.validate(new UpdateMaintenanceWindowScheduleRequest())).isEmpty();
    }

    @Test
    void acceptsPartialNameUpdate() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setName("nightly window");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsRecurrenceKindChangeWithMatchingFields() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsRecurrenceKindChangeWithoutRequiredFields() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setRecurrenceKind("CRON");

        Set<ConstraintViolation<UpdateMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidCronExpressionWhenProvidedWithoutRecurrenceKind() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setCronExpression("not a cron");

        Set<ConstraintViolation<UpdateMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void acceptsValidCronExpressionWhenProvidedWithoutRecurrenceKind() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setCronExpression("0 0 9 ? * MON *");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsRecurrenceKindChangeWithIncompatibleFields() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setRecurrenceKind("WEEKLY");
        request.setStartLocalTime("09:00");
        request.setDayOfWeek("MONDAY");
        request.setCronExpression("0 0 9 ? * MON *");

        Set<ConstraintViolation<UpdateMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }

    @Test
    void rejectsInvalidTimezoneWhenProvided() {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setTimezone("Not/A/Timezone");

        Set<ConstraintViolation<UpdateMaintenanceWindowScheduleRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "recurrenceConfigurationValid".equals(v.getPropertyPath().toString()));
    }
}
