package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MaintenanceWindowSkipRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsEmptySkipRequest() {
        assertThat(validator.validate(new MaintenanceWindowSkipRequest())).isEmpty();
    }

    @Test
    void acceptsReasonWithinLimit() {
        MaintenanceWindowSkipRequest request = new MaintenanceWindowSkipRequest();
        request.setReason("a".repeat(1024));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsReasonAboveLimit() {
        MaintenanceWindowSkipRequest request = new MaintenanceWindowSkipRequest();
        request.setReason("a".repeat(1025));

        Set<ConstraintViolation<MaintenanceWindowSkipRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "reason".equals(v.getPropertyPath().toString()));
    }
}
