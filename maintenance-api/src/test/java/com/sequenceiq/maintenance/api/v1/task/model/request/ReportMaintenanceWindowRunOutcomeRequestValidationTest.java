package com.sequenceiq.maintenance.api.v1.task.model.request;

import static com.sequenceiq.maintenance.api.validation.ValidationTestUtil.validator;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class ReportMaintenanceWindowRunOutcomeRequestValidationTest {

    private final Validator validator = validator();

    @Test
    void acceptsErrorDetailWithinLimit() {
        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("FAILED");
        request.setErrorDetail("a".repeat(1024));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsErrorDetailAboveLimit() {
        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("FAILED");
        request.setErrorDetail("a".repeat(1025));

        Set<ConstraintViolation<ReportMaintenanceWindowRunOutcomeRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> "errorDetail".equals(v.getPropertyPath().toString()));
    }
}
